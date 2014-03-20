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
    // Надо получить карту всех магазинов ТЦ. Это нужно для рендера фреймов.
    val shopsFut = MShop.findByMartId(martId, onlyEnabled=true)
      .map { _.map { shop => shop.id.get -> shop }.toMap }
    val mmartFut = MMart.getById(martId).map(_.get)
    IndicesUtil.getInxFormMartCached(martId) flatMap {
      case Some(mmartInx) =>
        for {
          ads <- MMartAdIndexed.findForLevel(AdShowLevels.LVL_MART_SHOWCASE, mmartInx)
          shops <- shopsFut
          mmart <- mmartFut
        } yield {
          val jsonHtml = JsObject(Seq(
            "html" -> indexTpl(mmart, ads, shops)
          ))
          Ok( Jsonp(JSONP_CB_FUN, jsonHtml) )
        }

      case None => NotFound("mart not indexed")
    }
  }

  /** Временный экшн, рендерит демо страничку предполагаемого сайта ТЦ, на которой вызывается Sio.Market */
  def demoWebSite(martId: MartId_t) = MaybeAuth.async { implicit request =>
    MMart.getById(martId) map {
      case Some(mmart) => Ok(demoWebsiteTpl(mmart))
      case None => NotFound("martNotFound")
    }
  }

}
