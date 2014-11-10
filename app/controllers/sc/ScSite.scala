package controllers.sc

import controllers.{routes, SioController}
import util.PlayMacroLogsI
import util.showcase._
import util.acl._
import views.html.market.showcase._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import play.api.mvc._
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 19:42
 * Description: Трейты с экшенами для рендера "сайтов" выдачи, т.е. html-страниц, возвращаемых при непоср.реквестах.
 * Бывает рендер через geo, который ищет подходящий узел, и рендер напрямую.
 */

/** Поддержка гео-сайта. */
trait ScSiteGeo extends SioController with PlayMacroLogsI with ScSiteConstants {

  /** Пользователь заходит в sio.market напрямую через интернет, без помощи сторонних узлов. */
  def geoSite = MaybeAuth.async { implicit request =>
    _geoSite
  }

  /**
   * Тело экшена _geoSite задаётся здесь, чтобы его можно было переопределять при необходимости.
   * @param request Экземпляр реквеста.
   * @return Результат работы экшена.
   */
  protected def _geoSite(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    // Запускаем сбор статистики в фоне.
    _geoSiteStats
    // Запускаем выдачу результата запроса:
    _geoSiteResult
  }

  /** Фоновый сбор статистики. Можно переназначать. */
  protected def _geoSiteStats(implicit request: AbstractRequestWithPwOpt[_]): Future[_] = {
    val fut = Future {
      ScSiteStat(AdnSinks.SINK_GEO)
    }.flatMap {
      _.saveStats
    }
    fut.onFailure {
      case ex => LOGGER.warn("geoSite(): Failed to save statistics", ex)
    }
    fut
  }

  /**
   * Раздавалка "сайта" выдачи первой страницы. Можно переопределять, для изменения/расширения функционала.
   * @param request Реквест.
   */
  protected def _geoSiteResult(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    val args = SMDemoSiteArgs(
      showcaseCall = routes.MarketShowcase.geoShowcase(),
      bgColor = SITE_BGCOLOR_GEO,
      adnId = None
    )
    cacheControlShort {
      Ok(demoWebsiteTpl(args))
    }
  }


  /** Раньше выдача пряталась в /market/geo/site. Потом переехала на главную. */
  def rdrToGeoSite = Action { implicit request =>
    val call = routes.MarketShowcase.geoSite().url
    MovedPermanently(call)
  }

}


/** Поддержка node-сайтов. */
trait ScSiteNode extends SioController with PlayMacroLogsI with ScSiteConstants {

  /**
   * Общий код для "сайтов" выдачи, относящихся к конкретным узлам adn.
   * @param showcaseCall Call для обращения за indexTpl.
   * @param request Исходный реквест, содержащий в себе необходимый узел adn.
   * @return 200 OK с рендером подложки выдачи.
   */
  protected def adnNodeDemoWebsite(showcaseCall: Call)(implicit request: AbstractRequestForAdnNode[AnyContent]) = {
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
      Future {
        ScSiteStat(AdnSinks.SINK_WIFI, Some(request.adnNode))
          .saveStats
          .onFailure {
            case ex => LOGGER.warn(s"demoWebSite($adnId): Failed to save stats", ex)
          }
      }
      // Рендерим результат в текущем потоке.
      adnNodeDemoWebsite(
        showcaseCall = routes.MarketShowcase.showcase(adnId)
      )

    } else {
      LOGGER.debug(s"demoWebSite($adnId): Requested node exists, but not available in public: enabled=$nodeEnabled ; isRcvr=$isReceiver")
      http404AdHoc
    }
  }

  /** Рендер страницы внутренней выдачи для указанного продьюсера.
    * Например, рекламодатель хочет посмотреть, как выглядят его карточки в выдаче. */
  def myAdsSite(adnId: String) = IsAdnNodeAdmin(adnId).apply { implicit request =>
    adnNodeDemoWebsite( routes.MarketShowcase.myAdsShowcase(adnId) )
  }

}


/** Константы лдя site-функционала. Используются и в разных трейтах. */
trait ScSiteConstants {

  /** Дефолтовое имя ноды. */
  val SITE_NAME_GEO = configuration.getString("market.showcase.nodeName.dflt") getOrElse "Suggest.io"

  /** Дефолтовый цвет выдачи, если нет ничего. */
  val SITE_BGCOLOR_DFLT = configuration.getString("market.showcase.color.bg.dflt") getOrElse "333333"

  val SITE_BGCOLOR_GEO = configuration.getString("market.showcase.color.bg.geo") getOrElse SITE_BGCOLOR_DFLT


  /** Дефолтовый цвет элементов переднего плана. */
  val SITE_FGCOLOR_DFLT = configuration.getString("market.showcase.color.fg.dflt") getOrElse "FFFFFF"

  /** Цвет для выдачи, которая вне узла. */
  val SITE_FGCOLOR_GEO = configuration.getString("market.showcase.color.fg.geo") getOrElse SITE_FGCOLOR_DFLT

}

