package io.suggest.lk.c

import io.suggest.captcha.CaptchaConstants
import io.suggest.lk.m.captcha.MCaptchaData
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.client.cache.{MHttpCacheInfo, MHttpCachingPolicies}
import io.suggest.proto.http.model.{HttpClientConfig, HttpReq, HttpReqData, HttpRespTypes}
import io.suggest.routes.routes
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.06.19 18:08
  * Description: API для доступа к капче.
  */
trait ICaptchaApi {

  /** Получение капчи с сервера.
    *
    * @return Фьючерс с данными капчи.
    */
  def getCaptcha(token: String): Future[MCaptchaData]

}

/** Реализация CaptchaApi поверх http. */
class CaptchaApiHttp( httpConfig: () => HttpClientConfig ) extends ICaptchaApi {

  override def getCaptcha(token: String): Future[MCaptchaData] = {
    for {
      // Запуск http-запрос за картинкой
      resp <- HttpClient.execute(
        HttpReq.routed(
          route = routes.controllers.Captcha.getCaptcha( token ),
          data  = HttpReqData(
            headers = {
              val C = HttpConst.Headers
              Map(
                C.ACCEPT -> "image/*"
              )
            },
            respType = HttpRespTypes.Blob,
            timeoutMs = Some( 10.seconds.toMillis.toInt ),
            cache = MHttpCacheInfo(
              policy = MHttpCachingPolicies.NetworkOnly
            ),
            config = httpConfig(),
          )
        )
      )
        .resultFut
        .successIf200

      blob <- resp.blob()

    } yield {
      MCaptchaData(
        imgData = blob,
        secret  = resp.getHeader( CaptchaConstants.CAPTCHA_SECRET_HTTP_HDR_NAME ).head
      )
    }
  }

}
