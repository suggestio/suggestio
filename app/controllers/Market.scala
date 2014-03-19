package controllers

import util._
import util.acl._
import views.html.market.showcase._
import play.api.libs.json._
import play.api.libs.Jsonp
import models._
import MMart.MartId_t, MShop.ShopId_t
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 11:37
 * Description: Выдача sio market
 */

object Market extends SioController with PlayMacroLogsImpl {

  val JSONP_CB_FUN = "siomart.receive_response"

  /** Входная страница для sio-market для ТЦ. */
  def martIndex(martId: MartId_t) = MaybeAuth.async { implicit request =>
    IndicesUtil.getInxFormMartCached(martId) flatMap {
      case Some(mmartInx) =>
        MMartAdIndexed.findForLevel(AdShowLevels.LVL_MART_SHOWCASE, mmartInx) map { ads =>
          val html = indexTpl(ads).toString.split("\n").map(_.trim.filter(_ >= ' ')).mkString
          val jsonHtml = JsObject(Seq(
            "html" -> JsString(html)
          ))
          Ok( Jsonp(JSONP_CB_FUN, jsonHtml) )
        }

      case None => NotFound("mart not indexed")
    }
  }

  /** Временный экшн, рендерит демо страничку предполагаемого сайта ТЦ, на которой вызывается Sio.Market */
  def demoWebsite = MaybeAuth { implicit request =>
    Ok(demoWebsiteTpl())
  }

}
