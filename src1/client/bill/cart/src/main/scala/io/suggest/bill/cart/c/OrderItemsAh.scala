package io.suggest.bill.cart.c

import diode.data.PendingBase
import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.bill.cart.m._
import io.suggest.bill.cart.v.itm.ItemRowPreviewR
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.msg.WarnMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.spa.DiodeUtil.Implicits._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.18 16:12
  * Description: Контроллер для корзинных экшенов.
  */
class OrderItemsAh[M](
                       lkCartApi        : ILkCartApi,
                       modelRW          : ModelRW[M, MOrderItemsS]
                     )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Клик по галочке item'а.
    case m: CartSelectItem =>
      val v0 = value
      val selItemIds2 = m.itemId.fold [Set[Gid_t]] {
        // Управление всеми элементами.
        if (m.checked) {
          v0.orderContents
            .iterator
            .flatMap(_.content.items)
            .flatMap(_.id)
            .toSet
        } else {
          Set.empty
        }

      } { itemId =>
        val itemIdSet = Set(itemId)
        // Управление одним элементом
        if (m.checked) {
          v0.itemsSelected ++ itemIdSet
        } else {
          v0.itemsSelected -- itemIdSet
        }
      }
      val v2 = MOrderItemsS.itemsSelected.set(selItemIds2)(v0)
      updated( v2 )


    // Нажатие по кнопке удаления выбранных item'ов.
    case CartDeleteBtnClick =>
      val v0 = value
      if (v0.itemsSelected.isEmpty || v0.orderContents.isPending) {
        noChange

      } else {
        // Запросить на сервере удаление, возвращающее обновлённый MOrderContent.
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
        val v2 = MOrderItemsS.orderContents.set( req2 )(v0)
        updated( v2, fx )
      }


    // Запуск запроса данных текущего ордера на сервер.
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
        val v2 = MOrderItemsS.orderContents.set(req2)(v0)
        updated( v2, fx )
      }


    // Обработка ответа по данным на текущий ордер.
    case m: HandleOrderContentResp =>
      val v0 = value
      if (v0.orderContents isPendingWithStartTime m.timestampMs) {
        val req2 = v0.orderContents
          .withTry( m.tryResp.map(MOrderContentJs.apply) )

        val v2 = v0.copy(
          orderContents = req2,
          jdRuntime = ItemRowPreviewR.mkJdRuntime(
            req2.iterator
              .flatMap(_.content.adsJdDatas)
              .map(_.doc)
              .toStream
          ),
          // Сброс (перефильтровать?) выделенных элементов, т.к. список item'ов изменился.
          itemsSelected =
            if (req2.isFailed) v0.itemsSelected
            else Set.empty,
        )
        updated( v2 )

      } else {
        LOG.warn( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m )
        noChange
      }

  }

}
