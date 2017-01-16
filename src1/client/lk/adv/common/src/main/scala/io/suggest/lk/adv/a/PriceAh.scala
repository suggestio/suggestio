package io.suggest.lk.adv.a

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.bill.MGetPriceResp
import io.suggest.lk.adv.m.{MPriceS, ResetPrice, SetPrice}
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.01.17 22:07
  * Description: Diode action handler для ценника.
  */
class PriceAh[M](
                  modelRW       : ModelRW[M, MPriceS],
                  priceAskFutF  : () => Future[MGetPriceResp]
                )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Экшен для запуска запроса рассчёта стоимости к серверу.
    case ResetPrice =>
      val ts = System.currentTimeMillis()
      val fx = Effect {
        for (resp <- priceAskFutF()) yield {
          SetPrice(resp, ts)
        }
      }

      val v0 = value
      val v2 = value.copy(
        reqTsId = Some(ts),
        resp    = v0.resp.pending(ts)
      )

      updated(v2, fx)

    // Сигнал о выполнении запроса рассчёта стоимости.
    case sp: SetPrice =>
      val v0 = value

      // Проверять актуальность запроса по timestamp из состояния...
      if ( v0.reqTsId.contains(sp.ts) ) {
        // Это нужный запрос.
        val v1 = v0.copy(
          reqTsId = None,
          resp = v0.resp.ready( sp.resp )
        )
        updated( v1 )

      } else {
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = sp )
        noChange
      }

  }

}
