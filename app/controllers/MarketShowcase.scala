package controllers

import SioControllerUtil.PROJECT_CODE_LAST_MODIFIED
import io.suggest.model.geo.GeoDistanceQuery
import util.stat._
import io.suggest.event.subscriber.SnFunSubscriber
import io.suggest.event.{AdnNodeSavedEvent, SNStaticSubscriber}
import io.suggest.event.SioNotifier.{Subscriber, Classifier}
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.i18n.Messages
import play.api.cache.Cache
import play.twirl.api.HtmlFormat
import util.ShowcaseUtil._
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
import play.api.mvc.{Cookie, Call, Result, AnyContent}
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
      title         = request.adnNode.meta.name,
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
      title = Messages("showcase.site.geo.title"),
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
    renderNodeShowcaseSimple(request.adnNode)
  }

  /** Рендер отображения выдачи узла. */
  private def renderNodeShowcaseSimple(adnNode: MAdnNode)(implicit request: AbstractRequestWithPwOpt[AnyContent]) = {
     val spsr = AdSearch(
      levels      = List(AdShowLevels.LVL_START_PAGE),
      receiverIds = List(adnNode.id.get)
    )
    val oncloseHref: String = adnNode.meta.siteUrl
      .filter { _ => ONCLOSE_HREF_USE_NODE_SITE }
      .getOrElse { ONCLOSE_HREF_DFLT }
    nodeShowcaseRender(adnNode, spsr, oncloseHref)
  }

  /** Выдача для продьюсера, который сейчас админят. */
  def myAdsShowcase(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    val spsr = AdSearch(
      producerIds = List(adnId)
    )
    // При закрытии выдачи, админ-рекламодатель должен попадать к себе в кабинет.
    val oncloseHref = Context.MY_AUDIENCE_URL + routes.MarketLkAdn.showAdnNode(adnId).url
    nodeShowcaseRender(request.adnNode, spsr, oncloseHref)
  }

  /** Рендер страницы-интерфейса поисковой выдачи. */
  private def nodeShowcaseRender(adnNode: MAdnNode, spsr: AdSearch, oncloseHref: String)(implicit request: AbstractRequestWithPwOpt[AnyContent]): Future[Result] = {
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
        mmcats      = mmcats,
        catsStats   = catsStats,
        spsr        = spsr,
        oncloseHref = oncloseHref,
        logoImgOpt  = adnNode.logoImgOpt,
        shops       = prods,
        welcomeAdOpt = waOpt
      )
      renderShowcase(args)
    }
  }


  /**
   * indexTpl для выдачи, отвязанной от конкретного узла.
   * Этот экшен на основе параметров думает на тему того, что нужно отрендерить. Может отрендерится showcase узла,
   * либо geoShowcase на дефолтовых параметрах.
   * @param args Аргументы.
   */
  def geoShowcase(args: SMShowcaseReqArgs) = MaybeAuth.async { implicit request =>
    args.geo.exactGeodata match {
      // Есть координаты текущие точные. Нужно поискать ближайший рекламный узел в рамках города.
      case Some(gp) =>
        val distanceMax = GeoIp.DISTANCE_DFLT
        val gdq = GeoDistanceQuery(center = gp, distanceMin = None, distanceMax = distanceMax)
        val nodeSearchArgs = MAdnNodeSearch(
          geoDistance = Some(gdq),
          maxResults = 1,
          withGeoDistanceSort = Some(gp)
        )
        MAdnNode.dynSearch(nodeSearchArgs) flatMap { nodes =>
          if (nodes.isEmpty) {
            // Нету узлов, подходящих под запрос.
            debug(s"geoShowcase($args): No nodes found nearby $gp in ${distanceMax.distance} ${distanceMax.units}")
            renderGeoShowcase(args)
          } else {
            // Нанооптимизация: начинаем рендер и возможных нарушениях работы ругаться в логи.
            val resultFut = renderNodeShowcaseSimple(nodes.head)
            val nodesCount = nodes.size
            if (nodesCount > 1) {
              // Should never occur:
              warn(s"geoShowcase($args): Unexpected count of nodes returned by search: $nodesCount, 0 or 1 collection length expected")
            }
            resultFut
          }
        }

      case None =>
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
        mmcats  = mmcats,
        catsStats = catsStats,
        spsr = AdSearch(
          levels = List(AdShowLevels.LVL_START_PAGE),
          geo    = GeoIp
        ),
        oncloseHref = ONCLOSE_HREF_DFLT
      )
      renderShowcase(args)
    }
  }


  /** Готовы данные для рендера showcase indexTpl. Сделать это и прочие сопутствующие операции. */
  private def renderShowcase(args: SMShowcaseRenderArgs)(implicit request: AbstractRequestWithPwOpt[AnyContent]): Result = {
    val html = indexTpl(args)
    val result = jsonOk("showcaseIndex", Some(html))
    StatUtil.resultWithStatCookie(result)
  }


  private def getCats(adnIdOpt: Option[String]) = {
    val catAdsSearch = AdSearch(
      receiverIds   = adnIdOpt.toList,
      maxResultsOpt = Some(100),
      levels        = List(AdShowLevels.LVL_MEMBERS_CATALOG)
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
    lazy val gsiFut = adSearch.geo.geoSearchInfo
    val (jsAction, adSearch2Fut) = if (adSearch.qOpt.isDefined) {
      "searchAds" -> Future.successful(adSearch)
    } else {
      // При поиске по категориям надо искать только если есть указанный show level.
      val adsearch3: Future[AdSearch] = if (adSearch.catIds.nonEmpty) {
        val result = adSearch.copy(levels = AdShowLevels.LVL_MEMBERS_CATALOG :: adSearch.levels)
        Future successful result
      } else if (adSearch.receiverIds.nonEmpty) {
        // TODO Можно спилить этот костыль?
        val lvls1 = (AdShowLevels.LVL_START_PAGE :: adSearch.levels).distinct
        val result = adSearch.copy(levels = lvls1)
        Future successful result
      } else if (adSearch.geo.isWithGeo) {
        gsiFut.flatMap {
          case None =>
            Future successful adSearch
          case Some(gsi) =>
            val nodeSearchArgs = MAdnNodeSearch(
              geoDistance = Option(gsi.geoDistanceQuery),
              maxResults = 1,
              withGeoDistanceSort = Some(gsi.geoPoint)
            )
            // Если узлы слишком далеко, то будет пустой список результатов.
            MAdnNode.dynSearchIds(nodeSearchArgs) map { nodeIds =>
              // TODO Если рекламных узлов по указанным координатам нет. То надо откатится на geoip? Или что отображать?
              trace(s"findAds(${request.path}): geo: nodeIds = ${nodeIds.mkString(", ")}")
              adSearch.copy(receiverIds = nodeIds.toList, geo = GeoNone)
            }
        } recover {
          // Допустима работа без геолокации при возникновении внутренней ошибки.
          case ex: Throwable =>
            error("findAds(): Failed to get geoip info for " + request.remoteAddress, ex)
            adSearch
        }
      } else {
        // Слегка неожиданные параметры запроса.
        warn("findAds(): strange search request: " + adSearch)
        Future successful adSearch
      }
      "findAds" -> adsearch3
    }
    val madsFut: Future[Seq[MAd]] = adSearch2Fut flatMap { adSearch2 =>
      trace("findAds(): Starting ads search using " + adSearch2)
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
    val tstamp = System.currentTimeMillis()
    for {
      sargs <- args.toSearchArgs
      nodes <- MAdnNode.dynSearch(sargs)
    } yield {
      val first_node = JsObject(Seq(
          "name"  -> JsString(nodes.head.meta.name),
          "_id"   -> JsString(nodes.head.id.getOrElse(""))
        ))
      val json = JsObject(Seq(
        "action"    -> JsString("findNodes"),
        "status"    -> JsString("ok"),
        "first_node"-> first_node,
        "nodes"     -> _geoNodesListTpl(nodes),
        "timestamp" -> JsNumber(tstamp)
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

