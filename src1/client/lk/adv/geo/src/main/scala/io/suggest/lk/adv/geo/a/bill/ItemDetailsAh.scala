package io.suggest.lk.adv.geo.a.bill

import diode._
import diode.data.Pending
import io.suggest.adv.geo.MFormS
import io.suggest.lk.adv.geo.m._
import io.suggest.lk.adv.geo.r.ILkAdvGeoApi
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.04.17 13:59
  * Description: Action handler для взаимодействия с компонентом стоимости.
  */
class ItemDetailsAh[M](
                 api      : ILkAdvGeoApi,
                 modelRW  : ModelRW[M, Option[MBillDetailedS]],
                 confRO   : ModelRO[MOther],
                 mFormRO  : ModelRO[MFormS]
                )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал показа/сокрытия деталей по item'у. Истинный посыл можно уточнить на основе данных состояния.
    case m: ShowHideItemPriceDetails =>
      val itemIndex = m.itemIndex

      value
        .filter { _.itemIndex == itemIndex }
        .fold {
          // Юзер желает узнать подробности по цене указанного item'а.
          val ts = System.currentTimeMillis()

          // Эффект запроса детализации с сервера.
          val fx = Effect {
            api
              .detailedPricing( confRO().adId, mFormRO(), itemIndex )
              .transform { tryRes =>
                val r = ItemDetailsResult(ts, itemIndex, tryRes)
                Success( r )
              }
          }

          // Выставить состояние ожидания ответа с сервера по item'у.
          val v2 = Some(
            MBillDetailedS(
              itemIndex = itemIndex,
              ts        = ts,
              req       = Pending()
            )
          )

          updated(v2, fx)

        } { _ =>
          // Повторный клик по цене того же item'а -- сокрытие текущего item'а.
          updated( None )
        }


    // Пришёл результат запроса к серверу
    case m: ItemDetailsResult =>
      val v0 = value
      if (v0.exists(_.ts == m.ts)) {
        // Это ожидаемый ответ сервера. Залить его в состояние.
        val v2 = for (v <- v0) yield {
          v.withReq(
            m.tryRes
              .fold( v.req.fail, v.req.ready )
          )
        }
        updated( v2 )

      } else {
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = m.ts )
        noChange
      }

  }

}
