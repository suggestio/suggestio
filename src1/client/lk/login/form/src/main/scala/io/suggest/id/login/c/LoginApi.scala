package io.suggest.id.login.c

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.id.MEpwLoginReq
import io.suggest.pick.MimeConst
import io.suggest.proto.http.HttpConst
import io.suggest.proto.http.client.HttpClient
import io.suggest.proto.http.model.{HttpReq, HttpReqData}
import io.suggest.routes.routes
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import play.api.libs.json.Json
import japgolly.univeq._

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.19 13:02
  * Description: API доступа к серверу.
  */
trait ILoginApi {

  /** Сабмит формы логина-пароля.
    *
    * @param loginReq Данные логина по паролю.
    * @param r Обратный (возвращаемый) редирект при положительном исходе.
    * @return Фьючерс с результатом.
    */
  def epw2Submit(loginReq: MEpwLoginReq, r: Option[String] = None): Future[String]

}


class LoginApiHttp extends ILoginApi {

  override def epw2Submit(loginReq: MEpwLoginReq, r: Option[String] = None): Future[String] = {
    // Собрать и запустить запрос:
    val respHolder = HttpClient.execute(
      HttpReq.routed(
        route = routes.controllers.Ident.epw2Submit( r.toUndef ),
        data  = HttpReqData(
          headers = {
            val H = HttpConst.Headers
            Map(
              H.CONTENT_TYPE -> MimeConst.APPLICATION_JSON,
              H.ACCEPT       -> MimeConst.TEXT_PLAIN,
            )
          },
          body = Json
            .toJson( loginReq )
            .toString(),
          timeoutMs = Some( 10.seconds.toMillis.toInt ),
        )
      )
    )
    // Распарсить ответ.
    for {
      resp <- respHolder.respFut
      if (resp.status ==* HttpConst.Status.OK)
      rdrUrl <- resp.text()
    } yield {
      rdrUrl
    }
  }

}
