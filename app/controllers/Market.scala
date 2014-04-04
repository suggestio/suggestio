package controllers

import util.qsb.AdSearch
import util._
import util.acl._
import views.html.market.showcase._
import play.api.libs.json._
import play.api.libs.Jsonp
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import SiowebEsUtil.client
import scala.concurrent.Future
import play.api.mvc.{AnyContent, Result}
import io.suggest.ym.model.stat.{MAdStat, AdStatActions}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 11:37
 * Description: Выдача sio market.
 */

object Market extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  val JSONP_CB_FUN = "siomart.receive_response"


  /** Входная страница для sio-market для ТЦ. */
  def martIndex(martId: MartId_t) = marketAction(martId) { implicit request =>
    for {
      mads <- MMartAdIndexed.find(request.mmartInx, AdSearch(levelOpt = Some(AdShowLevels.LVL_RECEIVER_TOP)))
      rmd  <- request.marketDataFut
    } yield {
      val html = indexTpl(request.mmart, mads, rmd.mshops, rmd.mmcats)
      jsonOk(html, "martIndex")
    }
  }

  /** Временный экшн, рендерит демо страничку предполагаемого сайта ТЦ, на которой вызывается Sio.Market */
  def demoWebSite(martId: MartId_t) = MaybeAuth.async { implicit request =>
    MMart.getById(martId) map {
      case Some(mmart) => Ok(demoWebsiteTpl(mmart))
      case None => NotFound("martNotFound")
    }
  }


  /** Выдать рекламные карточки в рамках ТЦ для категории и/или магазина. */
  def findAds(martId: MartId_t, adSearch: AdSearch) = marketAction(martId) { implicit request =>
    for {
      mads <- MMartAdIndexed.find(request.mmartInx, adSearch)
      rmd  <- request.marketDataFut
    } yield {
      val html = findAdsTpl(request.mmart, mads, rmd.mshops, rmd.mmcats)
      jsonOk(html, "findAds")
    }
  }


  // статистка

  /** Кем-то просмотрена одна рекламная карточка. */
  def adStats(martId: MartId_t, adId: String, actionRaw: String) = MaybeAuth.async { implicit request =>
    val action = AdStatActions.withName(actionRaw)
    MMartAd.getById(adId)
      .filter { _.exists(_.receiverIds == martId) }
      .flatMap { madOpt =>
        val adStat = MAdStat(
          clientAddr = request.remoteAddress,
          action = action,
          ua = request.headers.get(USER_AGENT),
          adId = adId,
          adOwnerId = madOpt.get.getOwnerId,
          personId = request.pwOpt.map(_.personId)
        )
        adStat.save.map { adStatId =>
          //adStat.id = Some(adStatId)
          //Created(adStatId)
          NoContent
        }
      }
  }


  // Внутренние хелперы

  private def shopsMap(martId: MartId_t): Future[Map[ShopId_t, MShop]] = {
    MShop.findByMartId(martId, onlyEnabled=true)
      .map { _.map { shop => shop.id.get -> shop }.toMap }
  }

  private def martNotFound(martId: MartId_t) = NotFound("Mart not found")

  /** Метод для генерации json-ответа с html внутри. */
  private def jsonOk(html: JsString, action: String) = {
    val jsonHtml = JsObject(Seq(
      "html"    -> html,
      "action"  -> JsString(action)
    ))
    Ok( Jsonp(JSONP_CB_FUN, jsonHtml) )
  }


  /** Action-composition для нужд ряда экшенов этого контроллера. Хранит в себе данные для рендере и делает
    * проверки наличия индекса и MMart. */
  private def marketAction(martId: MartId_t)(f: MarketRequest => Future[Result]) = MaybeAuth.async { implicit request =>
    // Смотрим метаданные по индексу маркета. Они обычно в кеше.
    val mmartInxOptFut = IndicesUtil.getInxFormMartCached(martId)
    // Надо получить карту всех магазинов ТЦ. Это нужно для рендера фреймов.
    val shopsFut = shopsMap(martId)
    // Читаем из основной базы текущий ТЦ
    val mmartFut = MMart.getById(martId)
    // Текущие категории ТЦ
    val mmcatsFut = MMartCategory.findTopForOwner(martId)
    mmartInxOptFut flatMap {
      case Some(mmartInx1) =>
        mmartFut flatMap {
          case Some(mmart1) =>
            val marketDataFut1 = for {
              mshops <- shopsFut
              mmcats <- mmcatsFut
            } yield {
              MarketData(mmcats, mshops)
            }
            val req1 = new MarketRequest(request) {
              val mmartInx = mmartInx1
              val marketDataFut = marketDataFut1
              val mmart = mmart1
            }
            f(req1)

          case None =>
            warn(s"marketAction($martId): mart index exists, but mart is NOT.")
            martNotFound(martId)
        }

      case None => martNotFound(martId)
    }
  }

  /** Реквест, используемый в Market-экшенах. */
  abstract class MarketRequest(request: AbstractRequestWithPwOpt[AnyContent])
    extends AbstractRequestWithPwOpt[AnyContent](request) {
    def mmartInx: MMartInx
    def mmart: MMart
    def marketDataFut: Future[MarketData]

    def sioReqMd: SioReqMd = request.sioReqMd
    def pwOpt: PersonWrapper.PwOpt_t = request.pwOpt
  }

  /** Контейнер асинхронно-получаемых данных, необходимых для рендера, но не нужных на промежуточных шагах. */
  case class MarketData(mmcats: Seq[MMartCategory], mshops: Map[ShopId_t, MShop])
}

