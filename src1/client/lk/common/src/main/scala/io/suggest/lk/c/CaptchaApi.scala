package io.suggest.lk.c

import io.suggest.captcha.CaptchaConstants
import io.suggest.lk.m.captcha.MCaptchaData
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.client.cache.{MHttpCacheInfo, MHttpCachingPolicies}
import io.suggest.proto.http.model.{HttpReq, HttpReqData, HttpRespTypes}
import io.suggest.routes.routes
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import org.scalajs.dom.raw.URL

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
  def getCaptcha(): Future[MCaptchaData]

}

/** Реализация CaptchaApi поверх http. */
class CaptchaApiHttp extends ICaptchaApi {

  override def getCaptcha(): Future[MCaptchaData] = {
    for {
      // Запуск http-запрос за картинкой
      resp <- HttpClient.execute(
        HttpReq.routed(
          route = routes.controllers.Img.getCaptcha(),
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
            )
          )
        )
      )
        .respFut
        .successIf200

      blob <- resp.blob()

    } yield {
      MCaptchaData(
        imgData = blob,
        blobUrl = URL.createObjectURL( blob ),
        secret  = resp.getHeader( CaptchaConstants.CAPTCHA_SECRET_HTTP_HDR_NAME ).get
      )
    }
  }

}
