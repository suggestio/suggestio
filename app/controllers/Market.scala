package controllers

import util.billing.StatBillingQueueActor
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
import play.api.mvc.RequestHeader
import io.suggest.ym.model.stat.{MAdStat, AdStatActions}
import io.suggest.ym.model.common.IBlockMeta
import play.api.Play.{current, configuration}
import play.api.templates.HtmlFormat

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.14 11:37
 * Description: Выдача sio market.
 */

object Market extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  val JSONP_CB_FUN = "siomart.receive_response"

  /** Максимальное кол-во магазинов, возвращаемых в списке ТЦ. */
  val MAX_SHOPS_LIST_LEN = configuration.getInt("market.frontend.subproducers.count.max") getOrElse 200

  /** Входная страница для sio-market для ТЦ. */
  def martIndex(adnId: String) = MaybeAuth.async { implicit request =>
    MAdnNodeCache.getByIdCached(adnId)
      .filter { _.isDefined }
      .flatMap { nodeOpt =>
        val adnNode = nodeOpt.get
        // Надо получить карту всех магазинов ТЦ. Это нужно для рендера фреймов.
        val allProdsFut = MAdnNode.findBySupId(adnId, onlyEnabled = true, maxResults = MAX_SHOPS_LIST_LEN)
          .map { _.map { prod => prod.id.get -> prod }.toMap }
        val prodsStatsFut = MAd.findProducerIdsForReceiver(adnId)
        // Текущие категории ТЦ
        val mmcatsFut = MMartCategory.findTopForOwner(getCatOwner(adnId))
        // Нужно отфильтровать магазины без рекламы.
        val shopsWithAdsFut = for {
          allProds    <- allProdsFut
          prodsStats  <- prodsStatsFut
        } yield {
          allProds.filterKeys( prodsStats contains )
        }
        val allAdsSearchReq = AdSearch(receiverIds = List(adnId), maxResultsOpt = Some(500))
        // Сборка статитстики по категориям нужна, чтобы подсветить серым пустые категории.
        val catsStatsFut = MAd.stats4UserCats(MAd.searchAdsReqBuilder(allAdsSearchReq))
          .map { _.toMap }
        val welcomeAdOptFut: Future[Option[MWelcomeAd]] = adnNode.meta.welcomeAdId match {
          case Some(waId) => MWelcomeAd.getById(waId)
          case None => Future successful None
        }
        val startPageSearchReq = AdSearch(
          levels = List(AdShowLevels.LVL_START_PAGE),
          receiverIds = List(adnId)
        )
        for {
          mads   <- MAd.searchAds(startPageSearchReq).map(groupNarrowAds)
          waOpt  <- welcomeAdOptFut
          catsStats <- catsStatsFut
          shops  <- shopsWithAdsFut
          mmcats <- mmcatsFut
        } yield {
          val html = indexTpl(adnNode, mads, shops, mmcats, waOpt, catsStats)
          jsonOk(html, "martIndex")
        }
      }
      .recover {
        case ex: NoSuchElementException =>
          warn(s"marketAction($adnId): node does not exist")
          martNotFound(adnId)
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
  def findAds(adSearch: AdSearch) = MaybeAuth.async { implicit request =>
    val producersFut = Future.traverse(adSearch.producerIds) { MAdnNodeCache.getByIdCached }
      .map { _.flatMap(_.toList) }
    for {
      mads      <- MAd.searchAds(adSearch).map(groupNarrowAds)
      producers <- producersFut
    } yield {
      val jsAction: String = if (adSearch.qOpt.isDefined) {
        "searchAds"
      } else if (!producers.isEmpty) {
        "producerAds"
      } else {
        "findAds"
      }
      // TODO Хвост списка продьюсеров дропается, для рендера используется только один. Надо бы в шаблоне отработать эту ситуацию.
      val html = findAdsTpl(mads, adSearch, producers.headOption)
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


  /**
   * Раздача страниц с текстами договоров sio-market.
   * @param clang Язык договора.
   * @return 200 Ok
   *         404 если текст договора не доступен на указанном языке.
   */
  def contractOfferText(clang: String) = MaybeAuth { implicit request =>
    import views.html.market.contract._
    val clangNorm = clang.toLowerCase.trim
    val textRenderOpt: Option[HtmlFormat.Appendable] = if (clangNorm startsWith "ru") {
      val render = textRuTpl()
      Some(render)
    } else {
      None
    }
    textRenderOpt match {
      case Some(render) => Ok(render)
      case None         => http404AdHoc
    }
  }


  // Внутренние хелперы

  private def martNotFound(martId: String)(implicit request: RequestHeader) = {
    info(s"martNotFound($martId): 404")
    http404AdHoc
  }

  /** Метод для генерации json-ответа с html внутри. */
  private def jsonOk(html: JsString, action: String) = {
    val json = JsObject(Seq(
      "html"    -> html,
      "action"  -> JsString(action)
    ))
    Ok( Jsonp(JSONP_CB_FUN, json) )
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


  def getCatOwner(adnId: String) = MMartCategory.DEFAULT_OWNER_ID

}

