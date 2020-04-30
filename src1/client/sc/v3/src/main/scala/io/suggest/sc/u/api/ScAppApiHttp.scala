package io.suggest.sc.u.api

import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model.{HttpReq, HttpReqData}
import io.suggest.routes.routes
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sc.app.{MScAppGetQs, MScAppGetResp}
import io.suggest.xplay.json.PlayJsonSjsUtil
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.02.2020 14:35
  * Description: API для ScApp-контроллера.
  */
trait IScAppApi {

  /** Получить инфу по скачиванию приложения.
    *
    * @param qs Данные для закачки.
    * @return Фьючерс с ссылкой внутри.
    */
  def appDownloadInfo( qs: MScAppGetQs ): Future[MScAppGetResp]

}


class ScAppApiHttp extends IScAppApi {

  override def appDownloadInfo(qs: MScAppGetQs): Future[MScAppGetResp] = {
    HttpClient.execute(
      HttpReq.routed(
        route = routes.controllers.ScApp.appDownloadInfo(
          PlayJsonSjsUtil.toNativeJsonObj(
            Json.toJsObject( qs )
          )
        ),
        data = HttpReqData(
          headers = HttpReqData.headersJsonSendAccept,
          timeoutMs = Some( 10.seconds.toMillis.toInt ),
        ),
      )
    )
      .httpResponseFut
      .successIf200
      .unJson[MScAppGetResp]
  }

}
