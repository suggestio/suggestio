package controllers

import io.suggest.model.EsModel.FieldsJsonAcc
import play.twirl.api.HtmlFormat
import util.ShowcaseUtil._
import util._
import util.acl._
import views.html.market.showcase._
import views.html.market.lk.adn._node._installScriptTpl
import play.api.libs.json._
import play.api.libs.Jsonp
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import SiowebEsUtil.client
import scala.concurrent.Future
import play.api.mvc.{Result, AnyContent}
import play.api.Play.{current, configuration}
/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 14:07
 * Description:
 */
object MarketShowcase extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  val JSONP_CB_FUN = "siomart.receive_response"

  /** Максимальное кол-во магазинов, возвращаемых в списке ТЦ. */
  val MAX_SHOPS_LIST_LEN = configuration.getInt("market.frontend.subproducers.count.max") getOrElse 200


  /** Экшн, который рендерит страничку приветствия, которое видит юзер при первом подключении к wi-fi */
  def demoWebSite(adnId: String) = AdnNodeMaybeAuth(adnId).apply { implicit request =>
    Ok(demoWebsiteTpl(request.adnNode))
  }

  /** Рендер интерфейса выдачи для указанного продьюсера. */
  def myAdsSite(adnId: String) = IsAdnNodeAdmin(adnId).apply { implicit request =>
    Ok(demoWebsiteTpl(
      request.adnNode,
      withIndexCall = Some(routes.MarketShowcase.myAdsShowcase(adnId))
    ))
  }

  /** Рендер скрипта выдачи для указанного узла. */
  def nodeSiteScript(adnId: String) = AdnNodeMaybeAuth(adnId).apply { implicit request =>
    Ok(_installScriptTpl(request.adnNode)) as "text/javascript"
  }


  /** Базовая выдача для rcvr-узла sio-market. */
  def showcase(adnId: String) = AdnNodeMaybeAuth(adnId).async { implicit request =>
    val spsr = AdSearch(
      levels      = List(AdShowLevels.LVL_START_PAGE),
      receiverIds = List(adnId)
    )
    commonShowcase(adnId, spsr)
  }

  /** Выдача для продьюсера, который сейчас админят. */
  def myAdsShowcase(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    val spsr = AdSearch(
      producerIds = List(adnId)
    )
    commonShowcase(adnId, spsr)
  }

  /** Рендер страницы-интерфейса поисковой выдачи. */
  private def commonShowcase(adnId: String, spsr: AdSearch)(implicit request: AbstractRequestForAdnNode[AnyContent]): Future[Result] = {
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
      receiverIds   = List(adnId),
      maxResultsOpt = Some(100),
      levels        = List(AdShowLevels.LVL_MEMBERS_CATALOG)
    )
    // Сборка статитстики по категориям нужна, чтобы подсветить серым пустые категории.
    val catsStatsFut = MAd.stats4UserCats(MAd.searchAdsReqBuilder(catAdsSearch))
      .map { _.toMap }
    val welcomeAdOptFut: Future[Option[MWelcomeAd]] = adnNode.meta.welcomeAdId match {
      case Some(waId) => MWelcomeAd.getById(waId)
      case None => Future successful None
    }
    for {
      waOpt     <- welcomeAdOptFut
      catsStats <- catsStatsFut
      shops     <- shopsWithAdsFut
      mmcats    <- mmcatsFut
    } yield {
      val html = indexTpl(adnNode, shops, mmcats, waOpt, catsStats, spsr)
      jsonOk("showcaseIndex", Some(html))
    }
  }


  /** Выдать рекламные карточки в рамках ТЦ для категории и/или магазина. */
  def findAds(adSearch: AdSearch) = MaybeAuth.async { implicit request =>
    val (jsAction, adSearch2) = if (adSearch.qOpt.isDefined) {
      "searchAds" -> adSearch
    } else {
      // При поиске по категориям надо искать только если есть указанный show level.
      val adsearch3 = if (adSearch.catIds.nonEmpty) {
        adSearch.copy(levels = AdShowLevels.LVL_MEMBERS_CATALOG :: adSearch.levels)
      } else if (adSearch.receiverIds.nonEmpty) {
        adSearch.copy(levels = AdShowLevels.LVL_START_PAGE :: adSearch.levels)
      } else {
        // Херота какая видимо.
        warn("findAds(): strange search request: " + adSearch)
        adSearch
      }
      "findAds" -> adsearch3
    }
    val madsFut: Future[Seq[MAd]] = MAd.searchAds(adSearch2)
    // Асинхронно вешаем параллельный рендер на найденные рекламные карточки.
    val madsRenderedFut = madsFut flatMap { mads0 =>
      val mads1 = groupNarrowAds(mads0)
      val ctx = implicitly[Context]
      parRenderBlocks(mads1) {
        (mad, index)  =>  _single_offer(mad, isWithAction = true)(ctx)
      }
    }
    madsRenderedFut map { madsRendered =>
      jsonOk(jsAction, blocks = madsRendered)
    }
  }


  /** Экшен для рендера горизонтальной выдачи карточек. */
  def focusedAds(adSearch: AdSearch) = MaybeAuth.async { implicit request =>
    val adSearch2 = if (adSearch.forceFirstIds.isEmpty) {
      adSearch
    } else {
      adSearch.copy(forceFirstIds = Nil)
    }
    val mads1Fut = MAd.searchAds(adSearch2)
    val producersFut = MAdnNodeCache.multiGet(adSearch.producerIds)
    val firstAdsFut = MAd.multiGet(adSearch.forceFirstIds)
      .map { _.filter {
        mad => adSearch.producerIds contains mad.producerId
      } }
    val mads2Fut: Future[Seq[MAd]] = mads1Fut flatMap { mads =>
      firstAdsFut map { firstAds =>
        val firstAdsIds = firstAds.map(_.id.get)
        // Если в mads, которые получились в результате поиска, уже содержаться те объявы, которые есть в firstAds, то выкинуть их из хвоста.
        val mads1 = mads filter {
          mad => !(firstAdsIds contains mad.id.get)
        }
        firstAds ++ mads1
      }
    }
    mads2Fut flatMap { mads =>
      producersFut flatMap { producers =>
        // Рендерим базовый html подвыдачи и рендерим остальные рекламные блоки отдельно, для ленивой подгрузки.
        val producer = producers.head
        val adsCount = mads.size
        // Распараллеливаем рендер блоков по всем ядрам (называется parallel map)
        val ctx = implicitly[Context]
        val blocksHtmlsFut = parRenderBlocks(mads.tail) {
          (mad, index)  =>  _focusedAdTpl(mad, index + 1, producer, adsCount = adsCount)(ctx)
        }
        // В текущем потоке рендерим основную HTML'ку, которая будет сразу отображена юзеру.
        val firstMads = mads.headOption.toList
        val html = JsString(_focusedAdsTpl(firstMads, adSearch, producer, adsCount = adsCount)(ctx))
        for {
          blocks <- blocksHtmlsFut
        } yield {
          jsonOk("producerAds", Some(html), blocks)
        }
      }
    }
  }


  /** Параллельный рендер scala-списка блоков на основе списка рекламных карточек.
    * @param mads список рекламных карточек.
    * @param r функция-рендерер, зависимая от контекста.
    * @return Фьючерс с результатом. Внутри список отрендеренных карточек в исходном порядке.
    */
  private def parRenderBlocks(mads: Seq[MAd])(r: (MAd, Int) => HtmlFormat.Appendable): Future[Seq[JsString]] = {
    val mads1 = mads.zipWithIndex
    Future.traverse(mads1) { case (mad, index) =>
      Future {
        index -> JsString(r(mad, index))
      }
    } map {
      _.sortBy(_._1).map(_._2)
    }
  }


  /** Метод для генерации json-ответа с html внутри. */
  private def jsonOk(action: String, html: Option[JsString] = None, blocks: Seq[JsString] = Nil) = {
    var acc: FieldsJsonAcc = Nil
    if (html.isDefined)
      acc ::= "html" -> html.get
    if (blocks.nonEmpty)
      acc ::= "blocks" -> JsArray(blocks)
    // action добавляем в начало списка
    acc ::= "action" -> JsString(action)
    val json = JsObject(acc)
    Ok( Jsonp(JSONP_CB_FUN, json) )
  }

}

