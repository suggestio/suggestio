package controllers

import java.util.NoSuchElementException

import SioControllerUtil.PROJECT_CODE_LAST_MODIFIED
import _root_.util.showcase.{ShowcaseNodeListUtil, ShowcaseUtil}
import io.suggest.model.geo.GeoShapeQueryData
import io.suggest.ym.model.common.AdnNodesSearchArgsWrapper
import util.stat._
import io.suggest.event.subscriber.SnFunSubscriber
import io.suggest.event.{AdnNodeSavedEvent, SNStaticSubscriber}
import io.suggest.event.SioNotifier.{Subscriber, Classifier}
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.cache.Cache
import play.twirl.api.HtmlFormat
import ShowcaseUtil._
import util._
import util.acl._
import views.html.market.showcase._
import views.txt.market.showcase.nodeIconJsTpl
import views.html.market.lk.adn._node._installScriptTpl
import play.api.libs.json._
import play.api.libs.Jsonp
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import SiowebEsUtil.client
import scala.concurrent.Future
import play.api.mvc.{RequestHeader, Call, Result, AnyContent}
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 14:07
 * Description: Контроллер выдачи sio-market.
 */
object MarketShowcase extends SioController with PlayMacroLogsImpl with SNStaticSubscriber {

  import LOGGER._

  val JSONP_CB_FUN = "siomart.receive_response"

  /** Максимальное кол-во магазинов, возвращаемых в списке ТЦ. */
  val MAX_SHOPS_LIST_LEN = configuration.getInt("market.frontend.subproducers.count.max") getOrElse 200

  /** Отображать ли пустые категории? */
  val SHOW_EMPTY_CATS = configuration.getBoolean("market.frontend.cats.empty.show") getOrElse false

  /** Дефолтовый цвет выдачи, если нет ничего. */
  val SITE_BGCOLOR_DFLT = configuration.getString("market.showcase.color.bg.dflt") getOrElse "333333"

  /** Дефолтовый цвет элементов переднего плана. */
  val SITE_FGCOLOR_DFLT = configuration.getString("market.showcase.color.fg.dflt") getOrElse "FFFFFF"

  /** Дефолтовое имя ноды. */
  val SITE_NAME_GEO = configuration.getString("market.showcase.nodeName.dflt") getOrElse "Suggest.io"


  /** Сколько времени кешировать скомпиленный скрипт nodeIconJsTpl. */
  val NODE_ICON_JS_CACHE_TTL_SECONDS = configuration.getInt("market.node.icon.js.cache.ttl.seconds") getOrElse 30
  val NODE_ICON_JS_CACHE_CONTROL_MAX_AGE: Int = {
    if (play.api.Play.isProd) {
      configuration.getInt("market.node.icon.js.cache.control.max.age") getOrElse 60
    } else {
      6
    }
  }

  /** Когда юзер закрывает выдачу, куда его отправлять, если отправлять некуда? */
  val ONCLOSE_HREF_DFLT = configuration.getString("market.showcase.onclose.href.dflt") getOrElse "http://yandex.ru/"

  /** Если true, то при наличии node.meta.site_url юзер при закрытии будет редиректиться туда.
    * Если false, то будет использоваться дефолтовый адрес для редиректа. */
  val ONCLOSE_HREF_USE_NODE_SITE = configuration.getBoolean("market.showcase.onclose.href.use.node.siteurl") getOrElse true

  /** Цвет для выдачи, которая вне узла. */
  val SITE_BGCOLOR_GEO = configuration.getString("market.showcase.color.bg.geo") getOrElse SITE_BGCOLOR_DFLT
  val SITE_FGCOLOR_GEO = configuration.getString("market.showcase.color.fg.geo") getOrElse SITE_FGCOLOR_DFLT

  /** id узла для демо-выдачи. */
  val DEMO_ADN_ID_OPT = configuration.getString("market.demo.adn.id")

  /** Сколько нод максимум накидывать к списку нод в качестве соседних нод. */
  val NEIGH_NODES_MAX = configuration.getInt("market.showcase.nodes.neigh.max") getOrElse 20


  /**
   * Общий код для "сайтов" выдачи, относящихся к конкретным узлам adn.
   * @param showcaseCall Call для обращения за indexTpl.
   * @param request Исходный реквест, содержащий в себе необходимый узел adn.
   * @return 200 OK с рендером подложки выдачи.
   */
  private def adnNodeDemoWebsite(showcaseCall: Call)(implicit request: AbstractRequestForAdnNode[AnyContent]) = {
    val args = SMDemoSiteArgs(
      showcaseCall  = showcaseCall,
      bgColor       = request.adnNode.meta.color getOrElse SITE_BGCOLOR_DFLT,
      title         = Some(request.adnNode.meta.name),
      adnId         = request.adnNode.id
    )
    Ok(demoWebsiteTpl(args))
  }

  /** Экшн, который рендерит страничку с дефолтовой выдачей узла. */
  def demoWebSite(adnId: String) = AdnNodeMaybeAuth(adnId).apply { implicit request =>
    adnNodeDemoWebsite( routes.MarketShowcase.showcase( adnId ) )
  }

  /** Рендер страницы внутренней выдачи для указанного продьюсера.
    * Например, рекламодатель хочет посмотреть, как выглядят его карточки в выдаче. */
  def myAdsSite(adnId: String) = IsAdnNodeAdmin(adnId).apply { implicit request =>
    adnNodeDemoWebsite( routes.MarketShowcase.myAdsShowcase(adnId) )
  }

  /** Пользователь заходит в sio.market напрямую через интернет, без помощи сторонних узлов. */
  def geoSite = MaybeAuth { implicit request =>
    val args = SMDemoSiteArgs(
      showcaseCall = routes.MarketShowcase.geoShowcase(),
      bgColor = SITE_BGCOLOR_GEO,
      adnId = None
    )
    Ok(demoWebsiteTpl(args))
  }


  /** Кеш-ключ для nodeIconJs. */
  private def nodeIconJsCacheKey(adnId: String) = adnId + ".nodeIconJs"

  /** Экшн, который рендерит скрипт с иконкой. Используется кеширование на клиенте и на сервере. */
  def nodeIconJs(adnId: String) = AdnNodeMaybeAuth(adnId).apply { implicit request =>
    // Проверяем ETag
    val isCachedEtag = request.adnNode.versionOpt
      .flatMap { vsn =>
        request.headers.get(IF_NONE_MATCH)
          .filter { etag  =>  vsn.toString == etag }
      }
      .nonEmpty
    // Проверяем Last-Modified, если ETag верен.
    val isCached = isCachedEtag && {
      request.headers.get(IF_MODIFIED_SINCE)
        .flatMap { DateTimeUtil.parseRfcDate }
        .exists { dt => !(PROJECT_CODE_LAST_MODIFIED isAfter dt)}
    }
    if (isCached) {
      NotModified
    } else {
      val ck = nodeIconJsCacheKey(adnId)
      Cache.getOrElse(ck, expiration = NODE_ICON_JS_CACHE_TTL_SECONDS) {
        var cacheHeaders: List[(String, String)] = List(
          CONTENT_TYPE  -> "text/javascript; charset=utf-8",
          LAST_MODIFIED -> DateTimeUtil.df.print(PROJECT_CODE_LAST_MODIFIED),
          CACHE_CONTROL -> ("public, max-age=" + NODE_ICON_JS_CACHE_CONTROL_MAX_AGE)
        )
        if (request.adnNode.versionOpt.isDefined) {
          cacheHeaders  ::=  ETAG -> request.adnNode.versionOpt.get.toString
        }
        // TODO Добавить минификацию скомпиленного js-кода. Это снизит нагрузку на кеш (на RAM) и на сеть.
        // TODO Добавить поддержку gzip надо бы.
        Ok(nodeIconJsTpl(request.adnNode))
          .withHeaders(cacheHeaders : _*)
      }
    }
  }

  /** Экшн, который выдает базовую инфу о ноде */
  def nodeData(adnId: String) = AdnNodeMaybeAuth(adnId).apply { implicit request =>
    val node = request.adnNode
    val logoSrcOpt = node.logoImgOpt map { logo_src =>
      val call = routes.Img.getImg(logo_src.filename)
      JsString(call.url)
    }
    val json = JsObject(Seq(
      "action"   -> JsString("setData"),
      "color"    -> node.meta.color.fold [JsValue] (JsNull) (JsString.apply),
      "logo_src" -> (logoSrcOpt getOrElse JsNull)
    ))
    Ok( Jsonp(JSONP_CB_FUN, json) )
  }

  /** Рендер скрипта выдачи для указанного узла. */
  def nodeSiteScript(adnId: String) = AdnNodeMaybeAuth(adnId).apply { implicit request =>
    Ok(_installScriptTpl( request.adnNode.id )) as "text/javascript"
  }


  /** Базовая выдача для rcvr-узла sio-market. */
  def showcase(adnId: String) = AdnNodeMaybeAuth(adnId).async { implicit request =>
    renderNodeShowcaseSimple(request.adnNode, isGeo = false)
  }

  /** Рендер отображения выдачи узла. */
  private def renderNodeShowcaseSimple(adnNode: MAdnNode, isGeo: Boolean)(implicit request: AbstractRequestWithPwOpt[AnyContent]) = {
     val spsr = AdSearch(
      levels      = List(AdShowLevels.LVL_START_PAGE),
      receiverIds = List(adnNode.id.get)
    )
    val oncloseHref: String = adnNode.meta.siteUrl
      .filter { _ => ONCLOSE_HREF_USE_NODE_SITE }
      .getOrElse { ONCLOSE_HREF_DFLT }
    nodeShowcaseRender(adnNode, spsr, oncloseHref, isGeo)
  }

  /** Выдача для продьюсера, который сейчас админят. */
  def myAdsShowcase(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    val spsr = AdSearch(
      producerIds = List(adnId)
    )
    // При закрытии выдачи, админ-рекламодатель должен попадать к себе в кабинет.
    val oncloseHref = Context.MY_AUDIENCE_URL + routes.MarketLkAdn.showAdnNode(adnId).url
    nodeShowcaseRender(request.adnNode, spsr, oncloseHref, isGeo = false)
  }

  /** Рендер страницы-интерфейса поисковой выдачи. */
  private def nodeShowcaseRender(adnNode: MAdnNode, spsr: AdSearch, oncloseHref: String, isGeo: Boolean)(implicit request: AbstractRequestWithPwOpt[AnyContent]): Future[Result] = {
    val adnId = adnNode.id.get
    // TODO Вынести сборку списка prods в отдельный экшен.
    // Нужно собрать продьюсеров рекламы. Собираем статистику по текущим размещениям, затем грабим ноды.
    val prodsStatsFut = MAd.findProducerIdsForReceiver(adnId)
    val prodsFut = prodsStatsFut flatMap { prodsStats =>
      val prodIds = prodsStats
        .iterator
        .filter { _._2 > 0 }
        .map { _._1 }
      MAdnNodeCache.multiGet(prodIds)
    } map { prodNodes =>
      prodNodes
        .map { adnNode => adnNode.id.get -> adnNode }
        .toMap
    }
    val (catsStatsFut, mmcatsFut) = getCats(adnNode.id)
    val welcomeAdOptFut: Future[Option[MWelcomeAd]] = adnNode.meta.welcomeAdId match {
      case Some(waId) => MWelcomeAd.getById(waId)
      case None => Future successful None
    }
    for {
      waOpt     <- welcomeAdOptFut
      catsStats <- catsStatsFut
      prods     <- prodsFut
      mmcats    <- mmcatsFut
    } yield {
      val args = SMShowcaseRenderArgs(
        bgColor     = adnNode.meta.color getOrElse SITE_BGCOLOR_DFLT,
        fgColor     = adnNode.meta.fgColor getOrElse SITE_FGCOLOR_DFLT,
        name        = adnNode.meta.name,
        mmcats      = mmcats,
        catsStats   = catsStats,
        spsr        = spsr,
        oncloseHref = oncloseHref,
        logoImgOpt  = adnNode.logoImgOpt,
        shops       = prods,
        welcomeAdOpt = waOpt
      )
      renderShowcase(args, isGeo, adnNode.id)
    }
  }


  /** Поиск узла на основе текущей геоинформации. */
  private def detectCurrentNodeByGeo[T](geo: GeoMode)(searchF: AdnNodesSearchArgsT => Future[Seq[T]])
                                       (implicit request: RequestHeader): Future[Option[T]] = {
    Future.traverse( geo.nodeGeoLevelsAlways.zipWithIndex ) {
      case (ngl, i) =>
        geo.geoSearchInfoOpt.flatMap { gsiOpt =>
          val nodeSearchArgs = new AdnNodesSearchArgs {
            override def geoDistance = gsiOpt.map { gsi => GeoShapeQueryData(gsi.geoDistanceQuery, ngl)}
            override def maxResults = 1
            override def withGeoDistanceSort = geo.exactGeodata
          }
          searchF(nodeSearchArgs) map { _ -> i }
        }
    } map { results =>
      //trace(s"detectCurrentNodeByGeo($geo): Matched geo results are:\n  ${results.mkString("\n  ")}")
      val resultsNonEmptyIter = results.iterator.filter(_._1.nonEmpty)
      if (resultsNonEmptyIter.isEmpty) {
        None
      } else {
        resultsNonEmptyIter.minBy(_._2)._1.headOption
      }
    }
  }


  /**
   * indexTpl для выдачи, отвязанной от конкретного узла.
   * Этот экшен на основе параметров думает на тему того, что нужно отрендерить. Может отрендерится showcase узла,
   * либо geoShowcase на дефолтовых параметрах.
   * @param args Аргументы.
   */
  def geoShowcase(args: SMShowcaseReqArgs) = MaybeAuth.async { implicit request =>
    lazy val logPrefix = s"geoShowcase(${System.currentTimeMillis}): "
    trace(logPrefix + "Starting, args = " + args)
    if (args.geo.isWithGeo) {
      detectCurrentNodeByGeo(args.geo)(MAdnNode.dynSearch) flatMap { nodeOpt =>
        if (nodeOpt.isEmpty) {
          // Нету узлов, подходящих под запрос.
          debug(logPrefix + "No nodes found nearby " + args.geo)
          renderGeoShowcase(args)
        } else {
          trace(logPrefix + "Choosen adn node according to geo is " + nodeOpt.flatMap(_.id))
          renderNodeShowcaseSimple(nodeOpt.get, isGeo = true)
        }
      }
    } else {
      renderGeoShowcase(args)
    }
  }
  private def renderGeoShowcase(args: SMShowcaseReqArgs)(implicit request: AbstractRequestWithPwOpt[AnyContent]) = {
    val (catsStatsFut, mmcatsFut) = getCats(None)
    for {
      mmcats    <- mmcatsFut
      catsStats <- catsStatsFut
    } yield {
      val args = SMShowcaseRenderArgs(
        bgColor = SITE_BGCOLOR_GEO,
        fgColor = SITE_FGCOLOR_GEO,
        name = SITE_NAME_GEO,
        mmcats  = mmcats,
        catsStats = catsStats,
        spsr = AdSearch(
          levels = List(AdShowLevels.LVL_START_PAGE),
          geo    = GeoIp
        ),
        oncloseHref = ONCLOSE_HREF_DFLT
      )
      renderShowcase(args, isGeo = true, currAdnId = None)
    }
  }


  /** Готовы данные для рендера showcase indexTpl. Сделать это и прочие сопутствующие операции. */
  private def renderShowcase(args: SMShowcaseRenderArgs, isGeo: Boolean, currAdnId: Option[String])
                            (implicit request: AbstractRequestWithPwOpt[AnyContent]): Result = {
    val html = indexTpl(args)
    val jsonArgs: FieldsJsonAcc = List(
      "is_geo"      -> JsBoolean(isGeo),
      "curr_adn_id" -> currAdnId.fold[JsValue](JsNull){ JsString.apply }
    )
    val result = jsonOk("showcaseIndex", Some(html), acc0 = jsonArgs)
    StatUtil.resultWithStatCookie(result)
  }


  private def getCats(adnIdOpt: Option[String]) = {
    val catAdsSearch = AdSearch(
      receiverIds   = adnIdOpt.toList,
      maxResultsOpt = Some(100),
      levels        = List(AdShowLevels.LVL_CATS)
    )
    // Сборка статитстики по категориям нужна, чтобы подсветить серым пустые категории.
    val catsStatsFut = MAd.stats4UserCats(MAd.dynSearchReqBuilder(catAdsSearch))
      .map { _.toMap }
    // Текущие категории узла
    val mmcatsFut: Future[Seq[MMartCategory]] = if(SHOW_EMPTY_CATS) {
      val catOwnerId = adnIdOpt.fold(MMartCategory.DEFAULT_OWNER_ID) { getCatOwner }
      MMartCategory.findTopForOwner(catOwnerId)
    } else {
      // Отключено отображение скрытых категорий. Исходя из статистики, прочитать из модели только необходимые карточки.
      catsStatsFut flatMap { catsStats =>
        MMartCategory.multiGet(catsStats.keysIterator)
          .map { _.sortBy(MMartCategory.sortByMmcat) }
      }
    }
    (catsStatsFut, mmcatsFut)
  }


  /** Выдать рекламные карточки в рамках ТЦ для категории и/или магазина.
    * @param adSearch Поисковый запрос.
    * @return JSONP с рекламными карточками для рендера в выдаче.
    */
  def findAds(adSearch: AdSearch) = MaybeAuth.async { implicit request =>
    lazy val logPrefix = s"findAds(${System.currentTimeMillis}):"
    trace(s"$logPrefix ${request.path}?${request.rawQueryString}")
    lazy val gsiFut = adSearch.geo.geoSearchInfoOpt
    val (jsAction, adSearch2Fut) = if (adSearch.qOpt.isDefined) {
      "searchAds" -> Future.successful(adSearch)
    } else {
      // При поиске по категориям надо искать только если есть указанный show level.
      val adsearch3: Future[AdSearch] = if (adSearch.catIds.nonEmpty) {
        val result = adSearch.copy(levels = AdShowLevels.LVL_CATS :: adSearch.levels)
        Future successful result
      } else if (adSearch.receiverIds.nonEmpty) {
        // TODO Можно спилить этот костыль?
        val lvls1 = (AdShowLevels.LVL_START_PAGE :: adSearch.levels).distinct
        val result = adSearch.copy(levels = lvls1)
        Future successful result
      } else if (adSearch.geo.isWithGeo) {
        // При геопоиске надо найти узлы, географически подходящие под запрос. Затем, искать карточки по этим узлам.
        detectCurrentNodeByGeo(adSearch.geo)(MAdnNode.dynSearchIds) map {
          case Some(adnId) =>
            adSearch.copy(receiverIds = List(adnId), geo = GeoNone)
          case None =>
            adSearch
        } recover {
          // Допустима работа без геолокации при возникновении внутренней ошибки.
          case ex: Throwable =>
            error(logPrefix + " Failed to get geoip info for " + request.remoteAddress, ex)
            adSearch
        }
      } else {
        // Слегка неожиданные параметры запроса.
        warn(logPrefix + " Strange search request: " + adSearch)
        Future successful adSearch
      }
      "findAds" -> adsearch3
    }
    val madsFut: Future[Seq[MAd]] = adSearch2Fut flatMap { adSearch2 =>
      trace(logPrefix + " Starting ads search using " + adSearch2)
      MAd.dynSearch(adSearch2)
    }
    // Асинхронно вешаем параллельный рендер на найденные рекламные карточки.
    val madsRenderedFut = madsFut flatMap { mads0 =>
      val mads1 = groupNarrowAds(mads0)
      val ctx = implicitly[Context]
      parRenderBlocks(mads1) {
        (mad, index)  =>  _single_offer(mad, isWithAction = true)(ctx)
      }
    }
    // Отрендеренные рекламные карточки нужно учитывать через статистику просмотров.
    madsFut onSuccess { case mads =>
      trace(s"$logPrefix Found ${mads.size} ads.")
      adSearch2Fut onSuccess { case adSearch2 =>
        AdStatUtil.saveAdStats(adSearch2, mads, AdStatActions.View, Some(gsiFut))
      }
    }
    // Асинхронно рендерим результат.
    madsRenderedFut map { madsRendered =>
      jsonOk(jsAction, blocks = madsRendered)
    }
  }

  /** Экшен для рендера горизонтальной выдачи карточек.
    * @param adSearch Поисковый запрос.
    * @param h true означает, что нужна начальная страница с html.
    *          false - возвращать только json-массив с отрендеренными блоками, без html-страницы с первой карточкой.
    * @return JSONP с отрендеренными карточками.
    */
  def focusedAds(adSearch: AdSearch, h: Boolean) = MaybeAuth.async { implicit request =>
    // TODO Не искать вообще карточки, если firstIds.len >= adSearch.size
    // TODO Выставлять offset для поиска с учётом firstIds?
    val mads1Fut = {
      // Костыль, т.к. сортировка forceFirstIds на стороне ES-сервера всё ещё не пашет:
      val adSearch2 = if (adSearch.forceFirstIds.isEmpty) {
        adSearch
      } else {
        adSearch.copy(forceFirstIds = Nil, withoutIds = adSearch.forceFirstIds)
      }
      MAd.dynSearch(adSearch2)
    }
    val madsCountFut = MAd.dynCount(adSearch)  // В countAds() можно отправлять и обычный adSearch: forceFirstIds там игнорируется.
    val producersFut = MAdnNodeCache.multiGet(adSearch.producerIds)
    // Если выставлены forceFirstIds, то нужно подолнительно запросить получение указанных id карточек и выставить их в начало списка mads1.
    val mads2Fut: Future[Seq[MAd]] = if (adSearch.forceFirstIds.nonEmpty) {
      // Если заданы firstIds и offset == 0, то нужно получить из модели указанные рекламные карточки.
      val firstAdsFut = if (adSearch.offset <= 0) {
        MAd.multiGet(adSearch.forceFirstIds)
          .map { _.filter {
            mad => adSearch.producerIds contains mad.producerId
          } }
      } else {
        Future successful Nil
      }
      // Замёржить полученные first-карточки в основной список карточек.
      for {
        mads      <- mads1Fut
        firstAds  <- firstAdsFut
      } yield {
        // Нано-оптимизация.
        if (firstAds.nonEmpty)
          firstAds ++ mads
        else
          mads
      }
    } else {
      // Дополнительно выставлять первые карточки не требуется. Просто возвращаем фьючерс исходного списка карточек.
      mads1Fut
    }
    // Когда поступят карточки, нужно сохранить по ним статистику.
    mads2Fut onSuccess { case mads =>
      AdStatUtil.saveAdStats(adSearch, mads, AdStatActions.Click, withHeadAd = h)
    }
    // Запустить рендер, когда карточки поступят.
    madsCountFut flatMap { madsCount =>
      val madsCountInt = madsCount.toInt
      producersFut flatMap { producers =>
        val producer = producers.head
        mads2Fut flatMap { mads =>
          // Рендерим базовый html подвыдачи (если запрошен) и рендерим остальные рекламные блоки отдельно, для отложенный инжекции в выдачу (чтобы подавить тормоза от картинок).
          val mads4renderAsArray = if (h) mads.tail else mads   // Caused by: java.lang.UnsupportedOperationException: tail of empty list
          val ctx = implicitly[Context]
          // Распараллеливаем рендер блоков по всем ядрам (называется parallel map). На 4ядернике (2 + HT) получается двукратный прирост на 33 карточках.
          val blocksHtmlsFut = parRenderBlocks(mads4renderAsArray, startIndex = adSearch.offset) {
            (mad, index) => _focusedAdTpl(mad, index + 1, producer, adsCount = madsCountInt)(ctx)
          }
          // В текущем потоке рендерим основную HTML'ку, которая будет сразу отображена юзеру. (если запрошено через аргумент h)
          val htmlOpt = if (h) {
            val firstMads = mads.headOption.toList
            val bgColor = producer.meta.color getOrElse SITE_BGCOLOR_DFLT
            val html = _focusedAdsTpl(firstMads, adSearch, producer, bgColor,  adsCount = madsCountInt,  startIndex = adSearch.offset)(ctx)
            Some(JsString(html))
          } else {
            None
          }
          for {
            blocks <- blocksHtmlsFut
          } yield {
            jsonOk("producerAds", htmlOpt, blocks)
          }
        }
      }
    }
  }


  /**
   * Отрендерить одну указанную карточку как веб-страницу.
   * @param adId id рекламной карточки.
   * @return 200 Ок с отрендеренной страницей-карточкой.
   */
  def standaloneBlock(adId: String) = MaybeAuth.async { implicit request =>
    // TODO Вынести логику read-набега на карточку в отдельный ACL ActionBuilder.
    MAd.getById(adId) map {
      case Some(mad) =>
        val bc: BlockConf = BlocksConf(mad.blockMeta.blockId)
        // TODO Проверять карточку на опубликованность?
        Ok( bc.renderBlock(mad, isStandalone = true) )

      case None =>
        warn(s"AdId $adId not found.")
        http404AdHoc
    }
  }


  /** Поиск узлов в рекламной выдаче. */
  def findNodes(args: SimpleNodesSearchArgs) = MaybeAuth.async { implicit request =>
    lazy val logPrefix = s"findNodes(${System.currentTimeMillis}): "
    trace(logPrefix + "Starting with args " + args + " ; remote = " + request.remoteAddress + " ; path = " + request.path + "?" + request.rawQueryString)
    // Для возможной защиты криптографических функций, использующий random, округяем и загрубляем timestamp.
    val tstamp = System.currentTimeMillis() / 50L

    // Запуск детектора текущей ноды, если необходимо. Асинхронно возвращает (lvl, node) или экзепшен.
    // Экзепшен означает, что переключение нод отключено или не удалось найти текущую ноду.
    val gsiOptFut = args.geoMode.geoSearchInfoOpt
    val nextNodeSwitchFut: Future[GeoDetectResult] = if (args.isNodeSwitch) {
      ShowcaseNodeListUtil.detectCurrentNode(args.geoMode, gsiOptFut)
    } else {
      Future failed new NoSuchElementException("Node detect disabled")
    }

    // Нода, которая будет отображена как текущая при следующем набеге на выдачу.
    val nextNodeFut = ShowcaseNodeListUtil.detectRecoverGuessCurrentNode(gsiOptFut, args.currAdnId)(nextNodeSwitchFut)



    val nodesLaysFut: Future[List[GeoNodesLayer]] = {
      // В зависимости от режима геолокации, надо произвести поиск узлов в разных слоях или вне их всех.
      val ngls = args.geoMode.nodeGeoLevelsAlways
      if (ngls.isEmpty) {
        // Если искомых геоуровней нет в текущем режиме, то и искать по географии не надо.
        debug(logPrefix + "No node geo levels available -- searching for all.")
        // Уровней поиска геоинформации нет. Ищем в лоб.
        args.toSearchArgs(None) flatMap { sargs =>
          MAdnNode.dynSearch(sargs)
        } map { nodes =>
          List(GeoNodesLayer(nodes))
        }
      } else {
        // Есть уровни для поиска. Надо запустить поиски узлов на разных уровнях.
        trace(logPrefix + "geo levels = " + ngls.mkString(", "))
        // На каждом уровне (сохраняя порядок уровней) поискать узлы, подходящие географически.
        val alwaysLaysFut = Future.traverse(ngls.zipWithIndex) {
          case (glevel, i) =>
            val someGlevel = Some(glevel)
            args.toSearchArgs(someGlevel)
              .flatMap { MAdnNode.dynSearch }
              .map { nodes =>
                trace(s"${logPrefix}On level $glevel found nodes ${nodes.iterator.map(_.id.get).mkString(", ")}")
                GeoNodesLayer(nodes, someGlevel, i)
              }
        }
        lazy val alwaysLaysCount = ngls.size
        val subNgls = args.geoMode.nodeGeoLevelsSub
          .iterator
          .zipWithIndex
          .map { case (ngl, i) => (ngl, i + alwaysLaysCount) }
          .toSeq
        val subLaysFut = Future.traverse(subNgls) {
          case (glevel, i) =>
            val someGlevel = Some(glevel)
            args.toSearchArgs(someGlevel)
              .flatMap { args0 =>
                nextNodeFut map { nextNode =>
                  new AdnNodesSearchArgsWrapper {
                    override def underlying = args0
                    override def withIds: Seq[String] = nextNode.id.toSeq
                  }
                }
              }
              .flatMap { MAdnNode.dynSearch }
              .map { nodes =>
                trace(s"${logPrefix}On level $glevel found nodes ${nodes.iterator.map(_.id.get).mkString(", ")}")
                Some(GeoNodesLayer(nodes, someGlevel, i))
              }
              .recover {
                case ex: NoSuchElementException => None
              }
        } map {
          _.flatMap(_.toSeq)
        }
        for {
          alwaysLays <- alwaysLaysFut
          subLays    <- subLaysFut
        } yield {
          (alwaysLays ++ subLays)
            .filter(_.nodes.nonEmpty)
            // Восстанавливаем порядок согласно списку гео-уровней.
            .sortBy(_.i)
        }
      }
    }

    def compileLays(laysFut: Future[Seq[GeoNodesLayer]]): Future[Seq[GeoNodesLayer]] = {
      for {
        lays        <- laysFut
        nextNode    <- nextNodeFut
      } yield {
        val nextNodeIdOpt = nextNode.id
        // Если текущая нода оказалась в нижнем слое, то нужно развернуть список слоёв.
        // Такое бывает в случае ноды-города.
        if (lays.lastOption.exists(_.nodes.exists(_.id == nextNodeIdOpt))) {
          lays.reverse
        } else {
          lays
        }
      }
    }

    // Если запрошены соседние узлы, то нужно запустить поиск оных, выбрав слои для исходного узла и для инжекции neigh-узлов.
    val nodesLays2Fut: Future[Seq[GeoNodesLayer]] = if (args.withNeighbors) {
      nodesLaysFut flatMap { nodeLays =>
        nextNodeFut flatMap { nextNode =>
          if (nodeLays.nonEmpty) {
            lazy val nodesCount = nodeLays.iterator.map(_.nodes.size).sum
            // Есть данные для заполнения списка узлов соседними узлами. Нужно модифицировать текущий уровень или нижний уровень
            val nextNodeLayOpt = nodeLays
              .find(_.nodes.exists(_.id == nextNode.id))

            // Следующая нода в нижнем гео-слое? Это влияет на определение родительского слоя.
            val isOnLowestGl = nextNodeLayOpt.exists { _.glevelOpt.exists(_.isLowest) }

            trace(s"${logPrefix}nodesFut: searching neigh nodes. isOnLowerGl=$isOnLowestGl nextNode=${nextNode.id.get} nodesCount=$nodesCount")

            val parentLayOpt: Option[GeoNodesLayer] = if (isOnLowestGl) {
              // Мы на самом нижнем уровне. Надо найти узел (узлы) уровнем выше текущего и запустить геопоиск узлов, входящих в него географически.
              // Найденные узлы закинуть на нижний уровень.
              nextNodeLayOpt.flatMap { currLay =>
                val upperLaysIter = nodeLays.iterator.filter(_.i > currLay.i)
                if (upperLaysIter.nonEmpty) {
                  Some(upperLaysIter.minBy(_.i))
                } else {
                  None
                }
              }
            } else {
              nextNodeLayOpt
            }

            // Определить подслой, в который надо добавлять найденные узлы.
            val childLayOpt: Option[(GeoNodesLayer, Boolean)] = if (isOnLowestGl) {
              nextNodeLayOpt.map { _ -> false }
            } else {
              parentLayOpt.flatMap { parentLay =>
                val lowerLayIter = nodeLays.iterator.filter(_.i > parentLay.i)
                if (lowerLayIter.nonEmpty) {
                  Some(lowerLayIter.maxBy(_.i) -> false)
                } else {
                  parentLay.glevelOpt
                    .flatMap(_.lower)
                    .filter { subNgl => args.geoMode.nodeGeoLevelsSub.contains(subNgl) }
                    .map { subNgl =>
                      GeoNodesLayer(nodes = Seq.empty, Some(subNgl), parentLay.i + 1) -> true
                    }
                }
              }
            }

            trace(s"${logPrefix}parentLay = ${parentLayOpt.flatMap(_.glevelOpt)} childLay = ${childLayOpt.flatMap(_._1.glevelOpt)} nextNode=${nextNode.id.get}")

            // Собираем соседние узлы по тому или иному алгоритму (в зависимости от настроек конфига).
            lazy val neighWithoutIds = nodeLays.iterator.flatMap(_.nodes).flatMap(_.id).toSeq.distinct
            val neighNodesSearchArgs: Future[Option[AdnNodesSearchArgsT]] = {
              // Извлекаем из parent-уровня id узлов и ищем их в поле parent-узлов.
              val sargsOpt = parentLayOpt
                .filter(_.nodes.nonEmpty)
                .map { parentLay =>
                  new AdnNodesSearchArgs {
                    override val withDirectGeoParents: Seq[String] = {
                      if (isOnLowestGl) {
                        parentLay.nodes.flatMap(_.id)
                      } else {
                        nextNode.id.toSeq
                      }
                    }
                    override def withoutIds = neighWithoutIds
                    override def withNameSort = true
                    override def maxResults: Int = NEIGH_NODES_MAX
                  }
                }
              Future successful sargsOpt
            }
            val neighNodesFut: Future[Seq[MAdnNode]] = neighNodesSearchArgs flatMap {
              case Some(sargs) =>
                println(sargs)
                MAdnNode.dynSearch(sargs)
              case None        => Future successful Seq.empty[MAdnNode]
            }

            // Инжектим найденные узлы в child-уровень, остальные просто пропускаем.
            val nodesLays2Fut = neighNodesFut map { neighNodes =>
              trace(logPrefix + "Neigh nodes found: " + neighNodes.iterator.flatMap(_.id).mkString(", "))
              if (childLayOpt.exists(_._2)) {
                nodeLays ++ Seq(childLayOpt.get._1.copy(nodes = neighNodes))
              } else {
                nodeLays.map { nodeLay =>
                  if (childLayOpt.exists(_._1.i == nodeLay.i)) {
                    // Инжектим соседние ноды в список нод текущего слоя
                    nodeLay.copy(nodes = nodeLay.nodes ++ neighNodes)
                  } else {
                    nodeLay
                  }
                }
              }
            }
            nodesLays2Fut

          } else {
            trace(s"${logPrefix}Neigh nodes search skipped. nextNode=${nextNode.id.get} nodeLays.nonEmpty=${nodeLays.nonEmpty}")
            nodesLaysFut
          }
        }
      }
    } else {
      trace(s"${logPrefix}Neigh nodes search disabled in args.")
      nodesLaysFut
    }

    val nodesLaysFut5 = compileLays(nodesLays2Fut)

    // Когда все данные будут собраны, нужно отрендерить результат в виде json.
    for {
      nodesLays5  <- nodesLaysFut5
      nextNode    <- nextNodeFut
    } yield {
      // Рендер в json следующего узла, если он есть.
      val nextNodeJson = JsObject(Seq(
        "name"  -> JsString(nextNode.meta.name),
        "_id"   -> JsString(nextNode.id getOrElse "")
      ))
      // Список узлов, который надо рендерить юзеру.
      val nodesRendered: Seq[GeoNodesLayer] = if (args.isNodeSwitch && nodesLays5.nonEmpty) {
        // При переключении узла, переключение идёт на наиболее подходящий узел, который первый в списке.
        // Тогда этот узел НЕ надо отображать в списке узлов.
        val nodes0 = nodesLays5.head.nodes
        nodesLays5.head.copy(nodes = nodes0.tail) :: nodesLays5.toList.tail
      } else {
        // Нет переключения узлов. Рендерим все подходящие узлы.
        nodesLays5
      }
      val json = JsObject(Seq(
        "action"      -> JsString("findNodes"),
        "status"      -> JsString("ok"),
        "first_node"  -> nextNodeJson,
        "nodes"       -> _geoNodesListTpl(nodesRendered, Some(nextNode)),
        "timestamp"   -> JsNumber(tstamp)
      ))
      Ok( Jsonp(JSONP_CB_FUN, json) )
    }
  }



  /** Параллельный рендер scala-списка блоков на основе списка рекламных карточек.
    * @param mads список рекламных карточек.
    * @param r функция-рендерер, зависимая от контекста.
    * @return Фьючерс с результатом. Внутри список отрендеренных карточек в исходном порядке.
    */
  private def parRenderBlocks(mads: Seq[MAd], startIndex: Int = 0)(r: (MAd, Int) => HtmlFormat.Appendable): Future[Seq[JsString]] = {
    val mads1 = mads.zipWithIndex
    Future.traverse(mads1) { case (mad, index) =>
      Future {
        index -> JsString(r(mad, startIndex + index))
      }
    } map {
      _.sortBy(_._1).map(_._2)
    }
  }


  /** Метод для генерации json-ответа с html внутри. */
  private def jsonOk(action: String, html: Option[JsString] = None, blocks: Seq[JsString] = Nil, acc0: FieldsJsonAcc = Nil) = {
    var acc: FieldsJsonAcc = acc0
    if (html.isDefined)
      acc ::= "html" -> html.get
    if (blocks.nonEmpty)
      acc ::= "blocks" -> JsArray(blocks)
    // action добавляем в начало списка
    acc ::= "action" -> JsString(action)
    val json = JsObject(acc)
    Ok( Jsonp(JSONP_CB_FUN, json) )
  }

  /** Карта статической подписки контроллера на некоторые события:
    * - Уборка из кеша рендера nodeIconJs. */
  override def snMap: Seq[(Classifier, Seq[Subscriber])] = {
    // Нужно чистить кеш nodeIconJs при обновлении узлов.
    val classifier = AdnNodeSavedEvent.getClassifier(isCreated = Some(false))
    val subscriber = SnFunSubscriber {
      case anse: AdnNodeSavedEvent =>
        val ck = nodeIconJsCacheKey(anse.adnId)
        Cache.remove(ck)
    }
    Seq(classifier -> Seq(subscriber))
  }


  /**
   * Постоянная ссылка на demo-выдачу, если она есть.
   * @return Редирект, если есть adn_id. 404 Если нет демо выдачи.
   */
  def demoShowcase = MaybeAuth { implicit request =>
    DEMO_ADN_ID_OPT match {
      case Some(adnId) =>
        Redirect(routes.MarketShowcase.demoWebSite(adnId))
      case None =>
        http404AdHoc
    }
  }

}

