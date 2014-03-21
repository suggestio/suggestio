package controllers

import util._
import util.acl._
import views.html.market.showcase._
import play.api.libs.json._
import play.api.libs.Jsonp
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import SiowebEsUtil.client
import scala.concurrent.Future
import play.api.mvc.SimpleResult

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
    val shopsFut = shopsMap(martId)
    // Читаем из основной базы текущий ТЦ
    val mmartFut = MMart.getById(martId).map(_.get)
    // Текущие категории ТЦ
    val mmcatsFut = MMartCategory.findTopForOwner(martId)
    // Смотрим метаданные по индексу маркета. Они обычно в кеше.
    IndicesUtil.getInxFormMartCached(martId) flatMap {
      case Some(mmartInx) =>
        for {
          ads    <- MMartAdIndexed.find(mmartInx, level = AdShowLevels.LVL_MART_SHOWCASE)
          shops  <- shopsFut
          mmart  <- mmartFut
          mmcats <- mmcatsFut
        } yield {
          val jsonHtml = JsObject(Seq(
            "html" -> indexTpl(mmart, ads, shops, mmcats)
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

  // TODO Нужно как-то дедублицировать повторяющийся код тут

  /** Выдать рекламные карточки в рамках ТЦ для категории и/или магазина. */
  def findAds(martId: MartId_t, shopIdOpt: Option[ShopId_t], catIdOpt: Option[String]) = MaybeAuth.async { implicit request =>
    val shopsFut = shopsMap(martId)
    val mmartFut = MMart.getById(martId).map(_.get)
    val mmcatsFut = MMartCategory.findTopForOwner(martId)
    IndicesUtil.getInxFormMartCached(martId) flatMap {
      case Some(mmartInx) =>
        val searchLevel = if (shopIdOpt.isDefined) {
          AdShowLevels.LVL_SHOP
        } else {
          AdShowLevels.LVL_MART_SHOPS
        }
        for {
          mads   <- MMartAdIndexed.find(mmartInx, shopIdOpt = shopIdOpt, catIdOpt = catIdOpt, level = searchLevel)
          mshops <- shopsFut
          mmart  <- mmartFut
          mmcats <- mmcatsFut
        } yield {
          Ok(indexTpl(mmart, mads, mshops, mmcats))
        }

      case None => martNotFound(martId)
    }
  }


  private def shopsMap(martId: MartId_t): Future[Map[ShopId_t, MShop]] = {
    MShop.findByMartId(martId, onlyEnabled=true)
      .map { _.map { shop => shop.id.get -> shop }.toMap }
  }

  private def martNotFound(martId: MartId_t) = NotFound("Mart not found")



}
