package controllers

import io.suggest.util.MacroLogsImpl
import util.acl._
import views.html.market.showcase._
import play.api.libs.json._
import play.api.libs.Jsonp

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 11:37
 * Description: Выдача sio market
 */

object Market extends SioController with MacroLogsImpl {

  val JSONP_CB_FUN = "siomart.receive_response"

  def index = MaybeAuth { implicit request =>

    val JsonObject = Json.toJson(
      Map("html" -> indexTpl().toString)
    )

    Ok( Jsonp(JSONP_CB_FUN, JsonObject ) )

  }

  /** Временный экшн, рендерит демо страничку предполагаемого сайта ТЦ, на которой вызывается Sio.Market */
  def demoWebsite = MaybeAuth { implicit request =>
    Ok(demoWebsiteTpl())
  }

}
