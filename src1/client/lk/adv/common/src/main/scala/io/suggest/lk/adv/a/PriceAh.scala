package io.suggest.lk.adv.a

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.bill.MGetPriceResp
import io.suggest.lk.adv.m._
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom.DomQuick

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.01.17 22:07
  * Description: Diode action handler для ценника.
  */
class PriceAh[M](
                  modelRW       : ModelRW[M, MPriceS],
                  priceAskFutF  : () => Future[MGetPriceResp],
                  doSubmitF     : () => Future[String]
                )
  extends ActionHandler(modelRW)
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Экшен для запуска запроса рассчёта стоимости к серверу.
    case ResetPrice =>
      val ts = System.currentTimeMillis()
      val fx = Effect {
        priceAskFutF().transformWith { tryResp =>
          Future.successful( HandleGetPriceResp(tryResp, ts) )
        }
      }

      val v0 = value
      val v2 = value.copy(
        reqTsId = Some(ts),
        resp    = v0.resp.pending(ts)
      )

      updated(v2, fx)


    // Сигнал о выполнении запроса рассчёта стоимости.
    case sp: HandleGetPriceResp =>
      val v0 = value
      // Проверять актуальность запроса по timestamp из состояния...
      if ( v0.reqTsId.contains(sp.ts) ) {
        // Это нужный запрос.
        val respPot2 = sp.tryResp match {
          case Success(res) =>
            v0.resp.ready( res )
          case Failure(ex) =>
            LOG.error( ErrorMsgs.XHR_UNEXPECTED_RESP, ex, sp )
            v0.resp.fail(ex)
        }
        val v1 = v0.copy(
          reqTsId = None,
          resp    = respPot2
        )
        updated( v1 )
      } else {
        LOG.log( WarnMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = sp )
        noChange
      }


    // Реакция на команду сабмита:
    case DoFormSubmit =>
      val fx = Effect {
        doSubmitF()
          .transformWith { tryRes =>
            Future.successful( HandleFormSubmitResp(tryRes) )
          }
      }
      // Блокируем виджет цены, имитируя запрос getPrice...
      val v0 = value
      val v1 = v0.withPriceResp(
        v0.resp.pending()
      )
      updated(v1, fx)


    // Реакция на ответ сервера по поводу сабмита формы.
    case hfsr: HandleFormSubmitResp =>
      hfsr.tryResp match {
        case Success(url) =>
          // Имитируем редирект на указанный сервером адресок. На этом заканчивается жизненный цикл текущей формы.
          DomQuick.goToLocation( url )
          noChange

        case Failure(ex) =>
          LOG.error( ErrorMsgs.XHR_UNEXPECTED_RESP, ex )
          // Разбокировать форму с помощью ошибки.
          val v0 = value
          val v1 = v0.withPriceResp(
            v0.resp.fail(ex)
          )
          updated(v1)
      }

  }

}
