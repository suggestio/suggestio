package io.suggest.sc.sjs.m.msrv

import io.suggest.sjs.common.model.Route
import io.suggest.sjs.common.xhr.Xhr

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSON

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.06.15 17:47
 * Description: Утиль для моделей, делающий доступ к серверу.
 */
object MSrvUtil {

  /**
   * HTTP-запрос через js-роутер и ожидание HTTP 200 Ok ответа.
   * @param route Маршрут jsrouter'а. Он содержит данные по URL и METHOD для запроса.
   * @return Фьючерс с десериализованным JSON.
   */
  def reqJson(route: Route)(implicit ec: ExecutionContext): Future[js.Dynamic] = {
    Xhr.successWithStatus(200) {
      Xhr.send(
        method  = route.method,
        url     = route.url,
        accept  = Some(Xhr.MIME_JSON)
      )
    } map { xhr =>
      JSON.parse(xhr.responseText)
    }
  }

}
