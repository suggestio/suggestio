package controllers.sc

import controllers.{routes, SioController}
import util.PlayMacroLogsI
import util.showcase._
import util.acl._
import util.xplay.SioHttpErrorHandler
import views.html.sc._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import play.api.mvc._
import ShowcaseUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 19:42
 * Description: Трейты с экшенами для рендера "сайтов" выдачи, т.е. html-страниц, возвращаемых при непоср.реквестах.
 * Бывает рендер через geo, который ищет подходящий узел, и рендер напрямую.
 */

/** Поддержка гео-сайта. */
trait ScSiteGeo extends SioController with PlayMacroLogsI {

  /** Пользователь заходит в sio.market напрямую через интернет, без помощи сторонних узлов. */
  def geoSite(maybeJsState: ScJsState) = MaybeAuth.async { implicit request =>
    if (maybeJsState.nonEmpty) {
      MovedPermanently( maybeJsState.ajaxStatedUrl() )
    } else {
      _geoSite
    }
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

  protected def _getSiteRenderArgs(implicit request: AbstractRequestWithPwOpt[_]): Future[ScSiteArgs] = {
    val args = new ScSiteArgs {
      override def showcaseCall = routes.MarketShowcase.geoShowcase()
      override def bgColor = SITE_BGCOLOR_GEO
    }
    Future successful args
  }

  /**
   * Раздавалка "сайта" выдачи первой страницы. Можно переопределять, для изменения/расширения функционала.
   * @param request Реквест.
   */
  protected def _geoSiteResult(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    _getSiteRenderArgs map { args =>
      cacheControlShort {
        Ok(siteTpl(args))
      }
    }
  }


  /** Раньше выдача пряталась в /market/geo/site. Потом переехала на главную. */
  def rdrToGeoSite = Action { implicit request =>
    val call = routes.MarketShowcase.geoSite().url
    MovedPermanently(call)
  }



}


/** Поддержка node-сайтов. */
trait ScSiteNode extends SioController with PlayMacroLogsI {

  /**
   * Общий код для "сайтов" выдачи, относящихся к конкретным узлам adn.
   * @param scCall Call для обращения за indexTpl.
   * @param request Исходный реквест, содержащий в себе необходимый узел adn.
   * @return 200 OK с рендером подложки выдачи.
   */
  protected def adnNodeDemoWebsite(scCall: Call)(implicit request: AbstractRequestForAdnNode[AnyContent]) = {
    val args = new ScSiteArgs {
      override def showcaseCall = scCall
      override val bgColor = request.adnNode.meta.color getOrElse SITE_BGCOLOR_DFLT
      override def nodeOpt = Some(request.adnNode)
    }
    cacheControlShort {
      Ok(siteTpl(args))
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
        scCall = routes.MarketShowcase.showcase(adnId)
      )

    } else {
      LOGGER.debug(s"demoWebSite($adnId): Requested node exists, but not available in public: enabled=$nodeEnabled ; isRcvr=$isReceiver")
      SioHttpErrorHandler.http404ctx
    }
  }

  /** Рендер страницы внутренней выдачи для указанного продьюсера.
    * Например, рекламодатель хочет посмотреть, как выглядят его карточки в выдаче. */
  def myAdsSite(adnId: String) = IsAdnNodeAdmin(adnId).apply { implicit request =>
    adnNodeDemoWebsite( routes.MarketShowcase.myAdsShowcase(adnId) )
  }

}


