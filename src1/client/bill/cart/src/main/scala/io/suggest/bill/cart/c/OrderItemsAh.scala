package io.suggest.bill.cart.c

import diode.data.PendingBase
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.bill.cart.m._
import io.suggest.bill.cart.u.CartUtil
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import io.suggest.spa.DiodeUtil.Implicits._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.18 16:12
  * Description: Controller for order-related actions.
  */
class OrderItemsAh[M](
                       lkCartApi        : ILkCartApi,
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
        val req2 = v0.orderContents
          .withTry( m.tryResp.map(MOrderContentJs.apply) )

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

  }

}
