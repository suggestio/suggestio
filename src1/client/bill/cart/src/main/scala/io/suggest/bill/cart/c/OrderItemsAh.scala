package io.suggest.bill.cart.c

import diode.data.{PendingBase, Pot}
import diode.{ActionHandler, ActionResult, Effect, ModelRO, ModelRW}
import io.suggest.bill.cart.MCartConf
import io.suggest.bill.cart.m._
import io.suggest.bill.cart.u.CartUtil
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import io.suggest.spa.DiodeUtil.Implicits._
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.18 16:12
  * Description: Controller for order-related actions.
  */
class OrderItemsAh[M](
                       lkCartApi        : => ILkCartApi,
                       confRO           : => ModelRO[MCartConf],
                       modelRW          : ModelRW[M, MOrderItemsS]
                     )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Item's selection checkbox changed its state.
    case m: CartSelectItem =>
      val v0 = value
      val selItemIds2 = m.itemId.fold [Set[Gid_t]] {
        // Overall all-items checkbox inside table header.
        if (m.checked) {
          (for {
            oc      <- v0.orderContents.iterator
            item    <- oc.content.items
            itemId  <- item.id
          } yield {
            itemId
          })
            .toSet

        } else {
          Set.empty
        }

      } { itemId =>
        // Checkbox for only one item.
        val itemIdSet = Set.empty + itemId
        if (m.checked) {
          v0.itemsSelected ++ itemIdSet
        } else {
          v0.itemsSelected -- itemIdSet
        }
      }
      val v2 = (MOrderItemsS.itemsSelected set selItemIds2)(v0)
      updated( v2 )


    // Delete items button pressed.
    case CartDeleteBtnClick =>
      val v0 = value
      if (v0.itemsSelected.isEmpty || v0.orderContents.isPending) {
        noChange

      } else {
        // Execute items deletion request on server:
        val req2 = v0.orderContents.pending()
        val timestampMs = req2.asInstanceOf[PendingBase].startTime
        val fx = Effect {
          lkCartApi
            .deleteItems( v0.itemsSelected )
            .transform { orderContentTry =>
              val action = HandleOrderContentResp( orderContentTry, timestampMs )
              Success( action )
            }
        }
        val v2 = (MOrderItemsS.orderContents set req2)(v0)
        updated( v2, fx )
      }


    // Ask server about order contents.
    case m: GetOrderContent =>
      val v0 = value
      if ( v0.orderContents.isPending ) {
        noChange

      } else {
        val req2 = v0.orderContents.pending()
        val timestampMs = req2.asInstanceOf[PendingBase].startTime
        val fx = Effect {
          lkCartApi
            .getOrder( m.orderId )
            .transform { orderContentTry =>
              val action = HandleOrderContentResp( orderContentTry, timestampMs )
              Success( action )
            }
        }
        val v2 = (MOrderItemsS.orderContents set req2)(v0)
        updated( v2, fx )
      }


    // GetOrderContent response reaction.
    case m: HandleOrderContentResp =>
      val v0 = value
      if (v0.orderContents isPendingWithStartTime m.timestampMs) {
        val req2 = v0.orderContents withTry m.tryResp.map(MOrderContentJs.apply)

        val v2 = v0.copy(
          orderContents = req2,
          jdRuntime = CartUtil.mkJdRuntime(
            (for {
              oc        <- req2.iterator
              adJdData  <- oc.content.adsJdDatas
            } yield
              adJdData.doc
            )
              .to( LazyList )
          ),
          // Reset (re-filter?) selected items, because items list updated.
          itemsSelected =
            if (req2.isFailed) v0.itemsSelected
            else Set.empty,
        )
        updated( v2 )

      } else {
        logger.warn( ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }


    // Unhold dialog open-close action:
    case m: UnHoldOrderDialogOpen =>
      val v0 = value

      if (m.isOpen && v0.unHoldOrder.isEmpty) {
        // Just open confirmation dialog
        val v2 = (
          MOrderItemsS.unHoldOrder.modify( _.ready(false) )
        )(v0)
        updated( v2 )
      } else if (!m.isOpen) {
        // Close opened dialog
        val v2 = (MOrderItemsS.unHoldOrder set Pot.empty)(v0)
        updated( v2 )
      } else {
        logger.log( ErrorMsgs.INACTUAL_NOTIFICATION, msg = (m, v0.unHoldOrder) )
        noChange
      }


    // Processing steps for unholding order: open/close dialog, request/response server, etc.
    case m: UnHoldOrderRequest =>
      val v0 = value

      m.tryRespOpt.fold[ActionResult[M]] {
        // HTTP request effect:
        val fx = Effect {
          val orderId = confRO.value.orderId.get
          lkCartApi
            .unHoldOrder( orderId )
            .transform { tryRes =>
              val action = m.copy( tryRespOpt = Some(tryRes) )
              Success( action )
            }
        }

        val v2 = MOrderItemsS.unHoldOrder.modify( _.pending(m.timestampMs) )(v0)
        updated( v2, fx )

      } { tryResp =>
        tryResp.fold(
          {ex =>
            // Request error occured. Display error inside dialog.
            logger.error( ErrorMsgs.SRV_REQUEST_FAILED, ex, (m, v0.unHoldOrder) )
            if (v0.unHoldOrder.isEmpty) {
              // dialog already closed. Nothing to do
              noChange
            } else {
              val v2 = MOrderItemsS.unHoldOrder.modify(_ fail ex)(v0)
              updated( v2 )
            }
          },
          {response =>
            // Update order with instance received.
            val v2 = (
              MOrderItemsS.unHoldOrder.set( Pot.empty ) andThen
              MOrderItemsS.orderContents.modify( _.ready( MOrderContentJs(response) ) )
            )(v0)
            updated(v2)
          }
        )
      }

  }

}
