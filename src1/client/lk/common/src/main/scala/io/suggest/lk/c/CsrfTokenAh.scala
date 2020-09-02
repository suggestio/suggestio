package io.suggest.lk.c

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import diode.data.Pot
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.lk.m.{CsrfTokenEnsure, CsrfTokenResp}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model.{HttpClientConfig, HttpReq, HttpReqData, MCsrfToken}
import io.suggest.routes.routes
import io.suggest.spa.DiodeUtil.Implicits._

import scala.concurrent.Future
import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.08.2020 12:18
  * Description: Контроллер для управления токеном.
  */
class CsrfTokenAh[M](
                      modelRW: ModelRW[M, Pot[MCsrfToken]],
                      csrfTokenApi: ICsrfTokenApi,
                    )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: CsrfTokenEnsure =>
      val v0 = value

      if (!m.force && (v0.isPending || v0.isReady)) {
        m.onComplete
          .fold(noChange)( effectOnly )

      } else {
        val tstamp = System.currentTimeMillis()
        val reqFx = Effect {
          csrfTokenApi
            .csrfToken()
            .transform { tryResp =>
              Success( CsrfTokenResp( tstamp, tryResp, m ) )
            }
        }
        val v2 = v0.pending( tstamp )
        updatedSilent( v2, reqFx )
      }


    case m: CsrfTokenResp =>
      val v0 = value

      if (!(v0 isPendingWithStartTime m.tstampMs)) {
        logger.warn( ErrorMsgs.SRV_RESP_INACTUAL_ANYMORE, msg = (m, v0) )
        m.reason.onComplete
          .fold(noChange)( effectOnly )

      } else {
        val v2 = v0 withTry m.tryResp
        ah.updatedMaybeEffect( v2, m.reason.onComplete )
      }

  }

}


trait ICsrfTokenApi {
  def csrfToken(): Future[MCsrfToken]
}
final class CsrfTokenApi(
                          httpClientConfig: () => HttpClientConfig,
                        )
  extends ICsrfTokenApi
{

  override def csrfToken(): Future[MCsrfToken] = {
    HttpClient.execute(
      HttpReq.routed(
        route = routes.controllers.Static.csrfToken(),
        data  = HttpReqData(
          headers = HttpReqData.headersJsonAccept,
          config  = httpClientConfig(),
        ),
      )
    )
      .resultFut
      .unJson[MCsrfToken]
  }

}
