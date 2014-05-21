package controllers

import _root_.util.billing.StatBillingQueueActor
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
import io.suggest.ym.model.common.IBlockMeta

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
  def martIndex(martId: String) = marketAction(martId) { implicit request =>
    val welcomeAdOptFut: Future[Option[MWelcomeAd]] = request.mmart.meta.welcomeAdId match {
      case Some(waId) => MWelcomeAd.getById(waId)
      case None => Future successful None
    }
    val searchReq = AdSearch(
      levelOpt = Some(AdShowLevels.LVL_START_PAGE),
      receiverIdOpt = Some(martId)
    )
    for {
      mads  <- MAd.searchAds(searchReq).map(groupNarrowAds)
      rmd   <- request.marketDataFut
      waOpt <- welcomeAdOptFut
    } yield {
      val html = indexTpl(request.mmart, mads, rmd.mshops, rmd.mmcats, waOpt)
      jsonOk(html, "martIndex")
    }
  }

  /** Временный экшн, рендерит демо страничку предполагаемого сайта ТЦ, на которой вызывается Sio.Market */
  def demoWebSite(martId: String) = MaybeAuth.async { implicit request =>
    MAdnNode.getById(martId) map {
      case Some(mmart) => Ok(demoWebsiteTpl(mmart))
      case None => NotFound("martNotFound")
    }
  }


  /** Выдать рекламные карточки в рамках ТЦ для категории и/или магазина. */
  def findAds(martId: String, adSearch: AdSearch) = marketAction(martId) { implicit request =>
    val producerOptFut = adSearch.producerIdOpt
      .fold [Future[Option[MAdnNode]]] { Future successful None } { MAdnNodeCache.getByIdCached }
    for {
      mads <- MAd.searchAds(adSearch).map(groupNarrowAds)
      rmd  <- request.marketDataFut
      producerOpt <- producerOptFut
    } yield {
      val jsAction: String = if (adSearch.qOpt.isDefined) {
        "searchAds"
      } else if (producerOpt.isDefined) {
        "producerAds"
      } else {
        "findAds"
      }
      val html = findAdsTpl(request.mmart, mads, rmd.mshops, rmd.mmcats, adSearch, producerOpt)
      jsonOk(html, jsAction)
    }
  }


  // статистка

  /** Кем-то просмотрена одна рекламная карточка. */
  def adStats(martId: String, adId: String, actionRaw: String) = MaybeAuth.apply { implicit request =>
    val action = AdStatActions.withName(actionRaw)
    MAd.getById(adId).map { madOpt =>
      madOpt.filter { mad =>
        mad.receivers.valuesIterator.exists(_.receiverId == martId)
      } foreach { mad =>
        StatBillingQueueActor.sendNewStats(rcvrId = martId, mad = mad, action = action)
        val adStat = MAdStat(
          clientAddr = request.remoteAddress,
          action = action,
          ua = request.headers.get(USER_AGENT),
          adId = adId,
          adOwnerId = mad.producerId,
          personId = request.pwOpt.map(_.personId)
        )
        adStat.save
      }
    }
    NoContent
  }


  // Внутренние хелперы

  private def shopsMap(martId: String): Future[Map[String, MAdnNode]] = {
    MAdnNode.findBySupId(martId, onlyEnabled=true)
      .map { _.map { shop => shop.id.get -> shop }.toMap }
  }

  private def martNotFound(martId: String) = NotFound("Mart not found")

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
  private def marketAction(martId: String)(f: MarketRequest => Future[Result]) = MaybeAuth.async { implicit request =>
    // Надо получить карту всех магазинов ТЦ. Это нужно для рендера фреймов.
    val shopsFut = shopsMap(martId)
    // Читаем из основной базы текущий ТЦ
    val mmartFut = MAdnNode.getById(martId)
    // Текущие категории ТЦ
    val mmcatsFut = MMartCategory.findTopForOwner(martId)
    mmartFut flatMap {
      case Some(mmart1) =>
        val marketDataFut1 = for {
          mshops <- shopsFut
          mmcats <- mmcatsFut
        } yield {
          MarketData(mmcats, mshops)
        }
        val req1 = new MarketRequest(request) {
          val marketDataFut = marketDataFut1
          val mmart = mmart1
        }
        f(req1)

      case None =>
        warn(s"marketAction($martId): mart index exists, but mart is NOT.")
        martNotFound(martId)
    }
  }


  /**
   * Сгруппировать "узкие" карточки, чтобы они были вместе.
   * @param ads Исходный список элементов.
   * @tparam T Тип элемента.
   * @return
   */
  private def groupNarrowAds[T <: IBlockMeta](ads: Seq[T]): Seq[T] = {
    val (enOpt1, acc0) = ads.foldLeft [(Option[T], List[T])] (None -> Nil) {
      case ((enOpt, acc), e) =>
        val blockId = e.blockMeta.blockId
        val bc: BlockConf = BlocksConf(blockId)
        if (bc.isNarrow) {
          enOpt match {
            case Some(en) =>
              (None, en :: e :: acc)
            case None =>
              (Some(e), acc)
          }
        } else {
          (enOpt, e :: acc)
        }
    }
    val acc1 = enOpt1 match {
      case Some(en) => en :: acc0
      case None     => acc0
    }
    acc1.reverse
  }


  /** Реквест, используемый в Market-экшенах. */
  abstract class MarketRequest(request: AbstractRequestWithPwOpt[AnyContent])
    extends AbstractRequestWithPwOpt[AnyContent](request) {
    def mmart: MAdnNode
    def marketDataFut: Future[MarketData]

    def sioReqMd: SioReqMd = request.sioReqMd
    def pwOpt: PersonWrapper.PwOpt_t = request.pwOpt
  }

  /** Контейнер асинхронно-получаемых данных, необходимых для рендера, но не нужных на промежуточных шагах. */
  case class MarketData(mmcats: Seq[MMartCategory], mshops: Map[String, MAdnNode])
}

