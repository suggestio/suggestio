package controllers.cstatic

import util.cdn.ICorsUtilDi
import controllers.SioController
import play.api.http.HeaderNames

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 11:56
 * Description: Трейт для контроллера для поддержки экшена, отвечающего на Cors Preflight.
 */
trait CorsPreflight
  extends SioController
  with ICorsUtilDi
{

  import mCommonDi.errorHandler

  /**
   * Реакция на options-запрос, хидеры выставит CORS-фильтр, подключенный в Global.
   * @param path Путь, к которому запрошены опшыны.
   * @return
   */
  def corsPreflight(path: String) = Action.async { implicit request =>
    val isEnabled = corsUtil.CORS_PREFLIGHT_ALLOWED

    if (isEnabled && request.headers.get( HeaderNames.ACCESS_CONTROL_REQUEST_METHOD ).nonEmpty) {
      // Кэшировать ответ на клиенте для ускорения работы системы. TODO Увеличить значение на неск.порядков:
      val cache = CACHE_CONTROL -> "public, max-age=300"
      val headers2 = cache :: corsUtil.PREFLIGHT_CORS_HEADERS
      Ok.withHeaders( headers2 : _* )

    } else {
      val body = if (isEnabled) "Missing nessesary CORS headers" else "CORS is disabled"
      errorHandler.onClientError(request, NOT_FOUND, body)
    }
  }

}
