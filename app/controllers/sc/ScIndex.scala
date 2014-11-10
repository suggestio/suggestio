package controllers.sc

import java.util.NoSuchElementException

import _root_.util.{Context, PlayMacroLogsI}
import models.Context
import util.img.WelcomeUtil
import util.showcase._
import util.stat._
import util.acl._
import util.SiowebEsUtil.client
import controllers.routes
import models.im.DevScreenT
import io.suggest.model.EsModel.FieldsJsonAcc
import ShowcaseUtil._
import views.html.market.showcase._
import play.api.libs.json._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import play.api.mvc._
import play.api.Play, Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.11.14 12:12
 * Description: Экшены, генерирующие indexTpl выдачи для узлов вынесены сюда.
 */
trait ScIndex extends ScController with PlayMacroLogsI with ScSiteConstants {

  /** Кеш ответа showcase(adnId) на клиенте. */
  val SC_INDEX_CACHE_SECONDS: Int = configuration.getInt("market.showcase.index.node.cache.client.seconds") getOrElse 20

  /** Если true, то при наличии node.meta.site_url юзер при закрытии будет редиректиться туда.
    * Если false, то будет использоваться дефолтовый адрес для редиректа. */
  val ONCLOSE_HREF_USE_NODE_SITE = configuration.getBoolean("market.showcase.onclose.href.use.node.siteurl") getOrElse true

  /** Когда юзер закрывает выдачу, куда его отправлять, если отправлять некуда? */
  val ONCLOSE_HREF_DFLT = configuration.getString("market.showcase.onclose.href.dflt") getOrElse "http://yandex.ru/"


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
      LOGGER.warn(s"showcase($adnId): failed to save stats, args = $args", ex)
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
    LOGGER.trace(logPrefix + "Starting, args = " + args)
    val gsiOptFut = args.geo.geoSearchInfoOpt
    val resultFut = if (args.geo.isWithGeo) {
      val gdrFut = ShowcaseNodeListUtil.detectCurrentNode(args.geo, gsiOptFut)
      gdrFut flatMap { gdr =>
        LOGGER.trace(logPrefix + "Choosen adn node according to geo is " + gdr.node.id.get)
        renderNodeShowcaseSimple(gdr.node,  isGeo = true,  geoListGoBack = Some(gdr.ngl.isLowest), screen = args.screen)
          .map { _ -> Some(gdr.node) }
      } recoverWith {
        case ex: NoSuchElementException =>
          // Нету узлов, подходящих под запрос.
          LOGGER.debug(logPrefix + "No nodes found nearby " + args.geo)
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
          LOGGER.warn("geoShowcase(): Failed to save statistics: args = " + args, ex)
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

}
