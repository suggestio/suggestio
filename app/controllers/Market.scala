package controllers

import util.billing.StatBillingQueueActor
import util.qsb.AdSearch
import util._
import util.acl._
import views.html.market.showcase._
import views.html.market.lk.adn._node._installScriptTpl
import views.html.market.aboutTpl
import play.api.libs.json._
import play.api.libs.Jsonp
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import SiowebEsUtil.client
import scala.concurrent.Future
import play.api.mvc.{Result, AnyContent, RequestHeader}
import io.suggest.ym.model.stat.{MAdStat, AdStatActions}
import io.suggest.ym.model.common.IBlockMeta
import play.api.Play.{current, configuration}
import play.api.templates.HtmlFormat
import io.suggest.model.EsModelMinimalT

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

  /** Базовая выдача для rcvr-узла sio-market. */
  def martIndex(adnId: String) = AdnNodeMaybeAuth(adnId).async { implicit request =>
    val spsr = AdSearch(
      levels      = List(AdShowLevels.LVL_START_PAGE),
      receiverIds = List(adnId)
    )
    adnNodeIndex(adnId, spsr)
  }

  /** Выдача для продьюсера, который сейчас админят. */
  def allMyAdsIndex(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    val spsr = AdSearch(
      producerIds = List(adnId)
    )
    adnNodeIndex(adnId, spsr)
  }

  private def adnNodeIndex(adnId: String, spsr: AdSearch)(implicit request: AbstractRequestForAdnNode[AnyContent]): Future[Result] = {
    val adnNode = request.adnNode
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
    val catAdsSearch = AdSearch(
      receiverIds = List(adnId),
      maxResultsOpt = Some(100),
      levels = List(AdShowLevels.LVL_MEMBERS_CATALOG)
    )
    // Сборка статитстики по категориям нужна, чтобы подсветить серым пустые категории.
    val catsStatsFut = MAd.stats4UserCats(MAd.searchAdsReqBuilder(catAdsSearch))
      .map { _.toMap }
    val welcomeAdOptFut: Future[Option[MWelcomeAd]] = adnNode.meta.welcomeAdId match {
      case Some(waId) => MWelcomeAd.getById(waId)
      case None => Future successful None
    }
    for {
      mads   <- MAd.searchAds(spsr).map(groupNarrowAds)
      waOpt  <- welcomeAdOptFut
      catsStats <- catsStatsFut
      shops  <- shopsWithAdsFut
      mmcats <- mmcatsFut
    } yield {
      val html = indexTpl(adnNode, mads, shops, mmcats, waOpt, catsStats)
      jsonOk(html, "martIndex")
    }
  }

  /** Экшн, который рендерит страничку приветствия, которое видит юзер при первом подключении к wi-fi */
  def demoWebSite(adnId: String) = AdnNodeMaybeAuth(adnId).async { implicit request =>
    Ok(demoWebsiteTpl(request.adnNode))
  }

  def allMyAdsSite(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    Ok(demoWebsiteTpl(
      request.adnNode,
      withIndexCall = Some(routes.Market.allMyAdsIndex(adnId))
    ))
  }

  def nodeSiteScript(adnId: String) = AdnNodeMaybeAuth(adnId).async { implicit request =>
    Ok(_installScriptTpl(request.adnNode)) as "text/javascript"
  }


  /** Выдать рекламные карточки в рамках ТЦ для категории и/или магазина. */
  def findAds(adSearch: AdSearch) = MaybeAuth.async { implicit request =>
    val isProducerAds = !adSearch.producerIds.isEmpty
    // TODO Использовать multiget + кеш.
    val producersFut = Future.traverse(adSearch.producerIds) { MAdnNodeCache.getByIdCached }
      .map { _.flatMap(_.toList) }
    val (jsAction, adSearch2) = if (adSearch.qOpt.isDefined) {
      "searchAds" -> adSearch
    } else if (isProducerAds) {
      //val _adSearch = adSearch.copy(levels = AdShowLevels.LVL_MEMBER :: adSearch.levels)
      "producerAds" -> adSearch
    } else if (!adSearch.catIds.isEmpty) {
      val _adSearch = adSearch.copy(levels = AdShowLevels.LVL_MEMBERS_CATALOG :: adSearch.levels)
      "findAds" -> _adSearch
    } else if (!adSearch.receiverIds.isEmpty) {
      val _adSearch = adSearch.copy(levels = AdShowLevels.LVL_START_PAGE :: adSearch.levels)
      // При поиске по категориям надо искать только если есть указанный show level.
      "findAds" -> _adSearch
    } else {
      // Херота какая видимо.
      warn("findAds(): strange search request: " + adSearch)
      "findAds" -> adSearch
    }
    val madsFut: Future[Seq[MAd]] = MAd.searchAds(adSearch2)
      .map { mads =>
        // Для producer ads надо не группировать узкие, а выносить firstAdIdOpt на первое место.
        if (isProducerAds) {
          mads
        } else {
          groupNarrowAds(mads)
        }
      }
    for {
      mads      <- madsFut
      producers <- producersFut
    } yield {
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
    val ctx = implicitly[Context]
    val textRenderOpt: Option[(HtmlFormat.Appendable, String)] = if (clangNorm startsWith "ru") {
      val render = textRuTpl()(ctx)
      Some(render -> "ru")
    } else {
      None
    }
    textRenderOpt match {
      case Some((render, clang2)) =>
        val fullRender = contractBase(clang2)(render)(ctx)
        Ok(fullRender)
      case None =>
        http404ctx(ctx)
    }
  }


  /** Статическая страничка, описывающая суть sio market. */
  def aboutMarket = MaybeAuth { implicit request =>
    Ok(aboutTpl())
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


  private def moveToHead[T <: EsModelMinimalT](src: Seq[T], headId: String): Seq[T] = {
    val inx = src.indexWhere { _.id.exists(_ == headId) }
    if (inx <= 0) {
      src
    } else {
      val e = src(inx)
      e :: src.drop(inx).toList
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


  def getCatOwner(adnId: String) = MMartCategory.DEFAULT_OWNER_ID

}

