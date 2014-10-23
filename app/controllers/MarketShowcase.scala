package controllers

import java.util.NoSuchElementException

import SioControllerUtil.PROJECT_CODE_LAST_MODIFIED
import _root_.util.img.WelcomeUtil
import _root_.util.showcase._
import io.suggest.ym.model.common.{IBlockMeta, EMBlockMetaI}
import models.blk.BlockHeights
import models.im.DevScreenT
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
import scala.concurrent.{Future, future}
import play.api.mvc._
import play.api.Play, Play.{current, configuration}

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

  // сбор категорий перенесён в ShowcaseUtil.

  /** Дефолтовый цвет выдачи, если нет ничего. */
  val SITE_BGCOLOR_DFLT = configuration.getString("market.showcase.color.bg.dflt") getOrElse "333333"

  /** Дефолтовый цвет элементов переднего плана. */
  val SITE_FGCOLOR_DFLT = configuration.getString("market.showcase.color.fg.dflt") getOrElse "FFFFFF"

  /** Дефолтовое имя ноды. */
  val SITE_NAME_GEO = configuration.getString("market.showcase.nodeName.dflt") getOrElse "Suggest.io"

  /** Сколько секунд следует кешировать переменную svg-картинку блока карточки. */
  def BLOCK_SVG_CACHE_SECONDS = configuration.getInt("market.showcase.blocks.svg.cache.seconds") getOrElse 700


  /** Сколько времени кешировать скомпиленный скрипт nodeIconJsTpl. */
  val NODE_ICON_JS_CACHE_TTL_SECONDS = configuration.getInt("market.node.icon.js.cache.ttl.seconds") getOrElse 30
  val NODE_ICON_JS_CACHE_CONTROL_MAX_AGE: Int = configuration.getInt("market.node.icon.js.cache.control.max.age") getOrElse {
    if (Play.isProd)  60  else  6
  }

  /** Когда юзер закрывает выдачу, куда его отправлять, если отправлять некуда? */
  val ONCLOSE_HREF_DFLT = configuration.getString("market.showcase.onclose.href.dflt") getOrElse "http://yandex.ru/"

  /** Если true, то при наличии node.meta.site_url юзер при закрытии будет редиректиться туда.
    * Если false, то будет использоваться дефолтовый адрес для редиректа. */
  val ONCLOSE_HREF_USE_NODE_SITE = configuration.getBoolean("market.showcase.onclose.href.use.node.siteurl") getOrElse true

  /** Цвет для выдачи, которая вне узла. */
  val SITE_BGCOLOR_GEO = configuration.getString("market.showcase.color.bg.geo") getOrElse SITE_BGCOLOR_DFLT
  val SITE_FGCOLOR_GEO = configuration.getString("market.showcase.color.fg.geo") getOrElse SITE_FGCOLOR_DFLT

  /** Сколько нод максимум накидывать к списку нод в качестве соседних нод. */
  val NEIGH_NODES_MAX = configuration.getInt("market.showcase.nodes.neigh.max") getOrElse 20

  /** Кеш ответа findNodes() на клиенте. Это существенно ускоряет навигацию. */
  val FIND_NODES_CACHE_SECONDS: Int = configuration.getInt("market.showcase.nodes.find.result.cache.seconds") getOrElse {
    if (Play.isProd)  120  else  10
  }

  /** Кеш ответа showcase(adnId) на клиенте. */
  val SC_INDEX_CACHE_SECONDS: Int = configuration.getInt("market.showcase.index.node.cache.client.seconds") getOrElse 20


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
    cacheControlShort {
      Ok(demoWebsiteTpl(args))
    }
  }

  /** Экшн, который рендерит страничку с дефолтовой выдачей узла. */
  def demoWebSite(adnId: String) = AdnNodeMaybeAuth(adnId).apply { implicit request =>
    val nodeEnabled = request.adnNode.adn.isEnabled
    val isReceiver = request.adnNode.adn.isReceiver
    if (nodeEnabled && isReceiver || request.isMyNode) {
      // Собираем статистику. Тут скорее всего wifi
      future {
        ScSiteStat(AdnSinks.SINK_WIFI, Some(request.adnNode))
          .saveStats
          .onFailure {
          case ex => warn(s"demoWebSite($adnId): Failed to save stats", ex)
        }
      }
      // Рендерим результат в текущем потоке.
      adnNodeDemoWebsite(
        showcaseCall = routes.MarketShowcase.showcase(adnId)
      )

    } else {
      debug(s"demoWebSite($adnId): Requested node exists, but not available in public: enabled=$nodeEnabled ; isRcvr=$isReceiver")
      http404AdHoc
    }
  }

  /** Рендер страницы внутренней выдачи для указанного продьюсера.
    * Например, рекламодатель хочет посмотреть, как выглядят его карточки в выдаче. */
  def myAdsSite(adnId: String) = IsAdnNodeAdmin(adnId).apply { implicit request =>
    adnNodeDemoWebsite( routes.MarketShowcase.myAdsShowcase(adnId) )
  }

  /** Пользователь заходит в sio.market напрямую через интернет, без помощи сторонних узлов. */
  def geoSite = MaybeAuth { implicit request =>
    // Запускаем сбор статистики в фоне.
    future {
      ScSiteStat(AdnSinks.SINK_GEO)
        .saveStats
        .onFailure {
          case ex => warn("geoSite(): Failed to save statistics", ex)
        }
    }
    val args = SMDemoSiteArgs(
      showcaseCall = routes.MarketShowcase.geoShowcase(),
      bgColor = SITE_BGCOLOR_GEO,
      adnId = None
    )
    val resultFut = cacheControlShort {
      Ok(demoWebsiteTpl(args))
    }

    resultFut
  }

  /** Раньше выдача пряталась в /market/geo/site. Потом переехала на главную. */
  def rdrToGeoSite = Action { implicit request =>
    val call = routes.MarketShowcase.geoSite().url
    MovedPermanently(call)
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
          LAST_MODIFIED -> DateTimeUtil.rfcDtFmt.print(PROJECT_CODE_LAST_MODIFIED),
          CACHE_CONTROL -> ("public, max-age=" + NODE_ICON_JS_CACHE_CONTROL_MAX_AGE)
        )
        if (request.adnNode.versionOpt.isDefined) {
          cacheHeaders  ::=  ETAG -> request.adnNode.versionOpt.get.toString
        }
        // TODO Добавить минификацию скомпиленного js-кода. Это снизит нагрузку на кеш (на RAM) и на сеть.
        // TODO Добавить поддержку gzip надо бы.
        // TODO Кешировать отрендеренные результаты на HDD, а не в RAM.
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
    cacheControlShort {
      Ok(Jsonp(JSONP_CB_FUN, json))
    }
  }

  /** Рендер скрипта выдачи для указанного узла. */
  def nodeSiteScript(adnId: String) = AdnNodeMaybeAuth(adnId).apply { implicit request =>
    Ok(_installScriptTpl(request.adnNode.id, mkPermanent = true))
      .withHeaders(
        CONTENT_TYPE  -> "text/javascript; charset=utf-8",
        CACHE_CONTROL -> "public, max-age=36000"
      )
  }


  /** Базовая выдача для rcvr-узла sio-market. */
  def showcase(adnId: String, args: SMShowcaseReqArgs) = AdnNodeMaybeAuth(adnId).async { implicit request =>
    val resultFut = MAdnNodeGeo.findIndexedPtrsForNode(adnId, maxResults = 1).flatMap { geos =>
      renderNodeShowcaseSimple(
        adnNode = request.adnNode,
        isGeo = false,  // Оксюморон с названием парамера. Все запросы гео-выдачи приходят в этот экшен, а геолокация отключена.
        geoListGoBack = geos.headOption.map(_.glevel.isLowest),
        screen = args.screen
      )
    }
    // собираем статистику, пока идёт подготовка результата
    val stat = ScIndexStatUtil(
      scSinkOpt = None,
      gsiFut    = args.geo.geoSearchInfoOpt,
      screenOpt = args.screen,
      nodeOpt   = Some(request.adnNode)
    )
    stat.saveStats onFailure { case ex =>
      warn(s"showcase($adnId): failed to save stats, args = $args", ex)
    }
    // Возвращаем результат основного действа. Результат вполне кешируем по идее.
    resultFut map { res =>
      res.withHeaders(CACHE_CONTROL -> s"public, max-age=$SC_INDEX_CACHE_SECONDS")
    }
  }

  /** Рендер отображения выдачи узла. */
  private def renderNodeShowcaseSimple(adnNode: MAdnNode, isGeo: Boolean, geoListGoBack: Option[Boolean] = None, screen: Option[DevScreenT] = None)
                                      (implicit request: AbstractRequestWithPwOpt[AnyContent]) = {
    val spsr = AdSearch(
      levels      = List(AdShowLevels.LVL_START_PAGE),
      receiverIds = List(adnNode.id.get)
    )
    val oncloseHref: String = adnNode.meta.siteUrl
      .filter { _ => ONCLOSE_HREF_USE_NODE_SITE }
      .getOrElse { ONCLOSE_HREF_DFLT }
    nodeShowcaseRender(adnNode, spsr, oncloseHref, isGeo, geoListGoBack = geoListGoBack, screen = screen)
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
  private def nodeShowcaseRender(adnNode: MAdnNode, spsr: AdSearch, oncloseHref: String, isGeo: Boolean,
                                 geoListGoBack: Option[Boolean] = None, screen: Option[DevScreenT] = None)
                                (implicit request: AbstractRequestWithPwOpt[AnyContent]): Future[Result] = {
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
    val ctx = implicitly[Context]
    val waOptFut = WelcomeUtil.getWelcomeRenderArgs(adnNode, screen)(ctx)
    for {
      waOpt     <- waOptFut
      catsStats <- catsStatsFut
      prods     <- prodsFut
      mmcats    <- mmcatsFut
    } yield {
      val args = SMShowcaseRenderArgs(
        searchInAdnId = (adnNode.geo.allParentIds -- adnNode.geo.directParentIds)
          .headOption
          .orElse(adnNode.geo.directParentIds.headOption)
          .orElse(adnNode.id),
        bgColor     = adnNode.meta.color getOrElse SITE_BGCOLOR_DFLT,
        fgColor     = adnNode.meta.fgColor getOrElse SITE_FGCOLOR_DFLT,
        name        = adnNode.meta.name,
        mmcats      = mmcats,
        catsStats   = catsStats,
        spsr        = spsr,
        oncloseHref = oncloseHref,
        logoImgOpt  = adnNode.logoImgOpt,
        shops       = prods,
        geoListGoBack = geoListGoBack,
        welcomeOpt  = waOpt
      )
      renderShowcase(args, isGeo, adnNode.id)(ctx)
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
    val gsiOptFut = args.geo.geoSearchInfoOpt
    val resultFut = if (args.geo.isWithGeo) {
      val gdrFut = ShowcaseNodeListUtil.detectCurrentNode(args.geo, gsiOptFut)
      gdrFut flatMap { gdr =>
        trace(logPrefix + "Choosen adn node according to geo is " + gdr.node.id.get)
        renderNodeShowcaseSimple(gdr.node,  isGeo = true,  geoListGoBack = Some(gdr.ngl.isLowest), screen = args.screen)
          .map { _ -> Some(gdr.node) }
      } recoverWith {
        case ex: NoSuchElementException =>
          // Нету узлов, подходящих под запрос.
          debug(logPrefix + "No nodes found nearby " + args.geo)
          renderGeoShowcase(args)
            .map { _ -> None }
      }
    } else {
      renderGeoShowcase(args)
        .map { _ -> None }
    }
    // Собираем статистику асинхронно
    resultFut onSuccess { case (result, nodeOpt) =>
      ScIndexStatUtil(Some(AdnSinks.SINK_GEO), gsiOptFut, args.screen, nodeOpt)
        .saveStats
        .onFailure { case ex =>
          warn("geoShowcase(): Failed to save statistics: args = " + args, ex)
        }
    }
    // Готовим настройки кеширования. Если геолокация по ip, то значит возможно только private-кеширование на клиенте.
    val (cacheControlMode, hdrs0) = if (!args.geo.isExact)
      "private" -> List(VARY -> X_FORWARDED_FOR)
    else
      "public" -> Nil
    val hdrs1 = CACHE_CONTROL -> s"$cacheControlMode, max-age=$SC_INDEX_CACHE_SECONDS"  ::  hdrs0
    // Возвращаем асинхронный результат, добавив в него клиентский кеш.
    resultFut
      .map { case (result, _) =>
        result.withHeaders(hdrs1 : _*)
      }
  }

  private def renderGeoShowcase(reqArgs: SMShowcaseReqArgs)(implicit request: AbstractRequestWithPwOpt[AnyContent]) = {
    val (catsStatsFut, mmcatsFut) = getCats(None)
    for {
      mmcats    <- mmcatsFut
      catsStats <- catsStatsFut
    } yield {
      val renderArgs = SMShowcaseRenderArgs(
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
      renderShowcase(renderArgs, isGeo = true, currAdnId = None)
    }
  }


  /** Готовы данные для рендера showcase indexTpl. Сделать это и прочие сопутствующие операции. */
  private def renderShowcase(args: SMShowcaseRenderArgs, isGeo: Boolean, currAdnId: Option[String])
                            (implicit ctx: Context): Result = {
    import ctx.request
    val html = indexTpl(args)(ctx)
    val jsonArgs: FieldsJsonAcc = List(
      "is_geo"      -> JsBoolean(isGeo),
      "curr_adn_id" -> currAdnId.fold[JsValue](JsNull){ JsString.apply }
    )
    // TODO Нужен аккуратный кеш тут. Проблемы с просто cache-control возникают, если список категорий изменился или
    // произошло какое-то другое изменение
    StatUtil.resultWithStatCookie {
      jsonOk("showcaseIndex", Some(html), acc0 = jsonArgs)
    }
  }



  /** Выдать рекламные карточки в рамках ТЦ для категории и/или магазина.
    * @param adSearch Поисковый запрос.
    * @return JSONP с рекламными карточками для рендера в выдаче.
    */
  def findAds(adSearch: AdSearch) = MaybeAuth.async { implicit request =>
    lazy val logPrefix = s"findAds(${System.currentTimeMillis}):"
    trace(s"$logPrefix ${request.path}?${request.rawQueryString}")
    // Геоданные нужны для собирания статистики.
    val gsiFut = adSearch.geo.geoSearchInfoOpt
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
        // TODO При таком поиске надо использовать cache-controle: private, если ip-геолокация.
        // При геопоиске надо найти узлы, географически подходящие под запрос. Затем, искать карточки по этим узлам.
        ShowcaseNodeListUtil.detectCurrentNode(adSearch.geo, adSearch.geo.geoSearchInfoOpt)
          .map { gdr => Some(gdr.node) }
          .recover { case ex: NoSuchElementException => None }
          .map {
            case Some(adnNode) =>
              adSearch.copy(receiverIds = List(adnNode.id.get), geo = GeoNone)
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
      MAd.dynSearch(adSearch2)
    }
    // Асинхронно вешаем параллельный рендер на найденные рекламные карточки.
    val madsRenderedFut = madsFut flatMap { mads0 =>
      val mads1 = groupNarrowAds(mads0)
      val ctx = implicitly[Context]
      parRenderBlocks(mads1) { (mad, index) =>
        Future {
          _single_offer(mad, isWithAction = true)(ctx)
        }
      }
    }
    // Отрендеренные рекламные карточки нужно учитывать через статистику просмотров.
    madsFut onSuccess { case mads =>
      adSearch2Fut onSuccess { case adSearch2 =>
        ScTilesStatUtil(adSearch2, mads.flatMap(_.id), gsiFut).saveStats
      }
    }
    // Асинхронно рендерим результат.
    madsRenderedFut map { madsRendered =>
      cacheControlShort {
        jsonOk(jsAction, blocks = madsRendered)
      }
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
      ScFocusedAdsStatUtil(adSearch, mads.flatMap(_.id), withHeadAd = h).saveStats
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
            (mad, index) =>
              ShowcaseUtil.focusedBrArgsFor(mad)(ctx) map { brArgs =>
                _focusedAdTpl(mad, index + 1, producer, adsCount = madsCountInt, brArgs = brArgs)(ctx)
              }
          }
          // В текущем потоке рендерим основную HTML'ку, которая будет сразу отображена юзеру. (если запрошено через аргумент h)
          val htmlOptFut = if (h) {
            val madsHead = mads.headOption
            val firstMads = madsHead.toList
            val bgColor = producer.meta.color getOrElse SITE_BGCOLOR_DFLT
            val brArgsNFut = madsHead.fold
              { Future successful ShowcaseUtil.focusedBrArgsDflt }
              { ShowcaseUtil.focusedBrArgsFor(_)(ctx) }
            brArgsNFut map { brArgsN =>
              val html = _focusedAdsTpl(firstMads, adSearch, producer, bgColor, brArgs = brArgsN, adsCount = madsCountInt,  startIndex = adSearch.offset)(ctx)
              Some(JsString(html))
            }
          } else {
            Future successful  None
          }
          for {
            blocks  <- blocksHtmlsFut
            htmlOpt <- htmlOptFut
          } yield {
            cacheControlShort {
              jsonOk("producerAds", htmlOpt, blocks)
            }
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
        cacheControlShort {
          Ok( bc.renderBlock(mad, blk.RenderArgs(isStandalone = true)) )
        }

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

    // Привести nextNodeFut к формату nextNodeSwitchFut.
    val nextNodeWithLayerFut = ShowcaseNodeListUtil.nextNodeWithLvlOptFut(nextNodeSwitchFut, nextNodeFut)

    // Когда все данные будут собраны, нужно отрендерить результат в виде json.
    val resultFut = for {
      nextNodeGdr <- nextNodeWithLayerFut
      nodesLays5  <- ShowcaseNodeListUtil.collectLayers(args.geoMode, nextNodeGdr.node, nextNodeGdr.ngl)
    } yield {
      import nextNodeGdr.{node => nextNode}
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
      // Без кеша, ибо timestamp.
      Ok( Jsonp(JSONP_CB_FUN, json) )
        .withHeaders(
          CACHE_CONTROL -> s"public, max-age=$FIND_NODES_CACHE_SECONDS"
        )
    }

    // Одновременно собираем статистику по запросу:
    ScNodeListingStat(args, gsiOptFut)
      .saveStats
      .onFailure { case ex =>
        warn(logPrefix + "Failed to save stats", ex)
      }

    // Вернуть асинхронный результат.
    resultFut
  }



  /** Параллельный рендер scala-списка блоков на основе списка рекламных карточек.
    * @param mads список рекламных карточек.
    * @param r функция-рендерер, зависимая от контекста.
    * @return Фьючерс с результатом. Внутри список отрендеренных карточек в исходном порядке.
    */
  private def parRenderBlocks(mads: Seq[MAd], startIndex: Int = 0)(r: (MAd, Int) => Future[HtmlFormat.Appendable]): Future[Seq[JsString]] = {
    val mads1 = mads.zipWithIndex
    Future.traverse(mads1) { case (mad, index) =>
      r(mad, startIndex + index) map { result =>
        index -> JsString(result)
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

}

