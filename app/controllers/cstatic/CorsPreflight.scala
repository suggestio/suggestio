package controllers.cstatic

import play.api.mvc._
import util.cdn.{CorsUtil, CorsUtil2}

import controllers.SioController

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 11:56
 * Description: Трейт для контроллера для поддержки экшена, отвечающего на Cors Preflight.
 */
trait CorsPreflight extends SioController {

  /**
   * Реакция на options-запрос, хидеры выставит CORS-фильтр, подключенный в Global.
   * @param path Путь, к которому запрошены опшыны.
   * @return
   */
  def corsPreflight(path: String) = Action { implicit request =>
    val isEnabled = CorsUtil.CORS_PREFLIGHT_ALLOWED
    if (isEnabled && request.headers.get("Access-Control-Request-Method").nonEmpty) {
      Ok.withHeaders(
        CorsUtil2.PREFLIGHT_CORS_HEADERS : _*
      )
    } else {
      val body = if (isEnabled) "Missing nessesary CORS headers" else "CORS is disabled"
      NotFound(body)
    }
  }

}
