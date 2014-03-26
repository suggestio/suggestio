package controllers

import _root_.util.qsb.AdSearch
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
 * Description: Выдача sio market.
 */

object Market extends SioController with PlayMacroLogsImpl {

  val JSONP_CB_FUN = "siomart.receive_response"

  /** Входная страница для sio-market для ТЦ. */
  def martIndex(martId: MartId_t) = MaybeAuth.async { implicit request =>
    new MarketAction(martId) {
      def execute(mmartInx: models.MMartInx): Future[SimpleResult] = {
        for {
          ads    <- MMartAdIndexed.find(mmartInx, AdSearch(levelOpt = Some(AdShowLevels.LVL_MART_SHOWCASE)))
          shops  <- shopsFut
          mmart  <- mmartFut
          mmcats <- mmcatsFut
        } yield {
          val jsonHtml = JsObject(Seq(
            "html" -> indexTpl(mmart, ads, shops, mmcats),
            "action" -> JsString("martIndex")
          ))
          Ok( Jsonp(JSONP_CB_FUN, jsonHtml) )
        }
      }
    }.apply
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
  def findAds(martId: MartId_t, adSearch: AdSearch) = MaybeAuth.async { implicit request =>
    new MarketAction(martId) {
      def execute(mmartInx: models.MMartInx): Future[SimpleResult] = {
        for {
          mads   <- MMartAdIndexed.find(mmartInx, adSearch)
          mshops <- shopsFut
          mmart  <- mmartFut
          mmcats <- mmcatsFut
        } yield {
          val jsonHtml = JsObject(Seq(
            "html" -> findAdsTpl(mmart, mads, mshops, mmcats),
            "action" -> JsString("findAds")
          ))
          Ok( Jsonp(JSONP_CB_FUN, jsonHtml) )
        }
      }
    }.apply
  }


  private def shopsMap(martId: MartId_t): Future[Map[ShopId_t, MShop]] = {
    MShop.findByMartId(martId, onlyEnabled=true)
      .map { _.map { shop => shop.id.get -> shop }.toMap }
  }

  private def martNotFound(martId: MartId_t) = NotFound("Mart not found")


  /** Общий код разных экшенов, которые полностью рендерят интерфейс целиком. */
  abstract class MarketAction(martId: MartId_t) {
    // Надо получить карту всех магазинов ТЦ. Это нужно для рендера фреймов.
    val shopsFut = shopsMap(martId)
    // Читаем из основной базы текущий ТЦ
    val mmartFut = MMart.getById(martId).map(_.get)
    // Текущие категории ТЦ
    val mmcatsFut = MMartCategory.findTopForOwner(martId)
    // Смотрим метаданные по индексу маркета. Они обычно в кеше.
    val mmartInxOptFut = IndicesUtil.getInxFormMartCached(martId)

    def execute(mmartInx: MMartInx): Future[SimpleResult]

    def apply: Future[SimpleResult] = {
      mmartInxOptFut flatMap {
        case Some(mmartInx) =>
          execute(mmartInx)

        case None => martNotFound(martId)
      }
    }
  }

}
