package controllers.sc

import controllers.{routes, SioController}
import models.mext.MExtServices
import models.msc.{ScJsState, ScSiteArgs, SiteQsArgs}
import play.twirl.api.Html
import util.PlayMacroLogsI
import util.showcase._
import util.acl._
import util.xplay.SioHttpErrorHandler
import views.html.sc._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import play.api.mvc._
import util.SiowebEsUtil.client

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
  def geoSite(maybeJsState: ScJsState, siteArgs: SiteQsArgs) = MaybeAuth.async { implicit request =>
    if (maybeJsState.nonEmpty) {
      // Было раньше MovedPermanently, но почему-то оно может сбойнуть и закешироваться на CDN.
      LOGGER.trace("Qs js state is nonEmpty, redirecting... " + maybeJsState)
      Redirect {
        routes.MarketShowcase.geoSite(x = siteArgs).url + "#!?" + maybeJsState.toQs()
      }
    } else {
      _geoSite(siteArgs)
    }
  }

  /**
   * Тело экшена _geoSite задаётся здесь, чтобы его можно было переопределять при необходимости.
   * @param request Экземпляр реквеста.
   * @return Результат работы экшена.
   */
  protected def _geoSite(siteArgs: SiteQsArgs)(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    // Запускаем сбор статистики в фоне.
    _geoSiteStats
    // Запускаем выдачу результата запроса:
    _geoSiteResult(siteArgs)
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
   * Собрать параметры рендера шаблона siteTpl.
   * @param siteArgs qs-аргументы запроса, касающиеся сайта.
   * @param request Экземпляр реквеста.
   * @return Фьючерс с аргументами для шаблона.
   */
  protected def _getSiteRenderArgs(siteArgs: SiteQsArgs)(implicit request: AbstractRequestWithPwOpt[_]): Future[ScSiteArgs] = {
    // Что сформировать мета-теги, нужно найти карточку, к которой относится всё это дело.
    // А если нет POV-карточки (обычно её нет), то нужно втыкать данные для узла.
    implicit lazy val ctx = implicitly[Context]
    val headAftersFut = {
      MAd.maybeGetById( siteArgs.povAdId )
        .map { _.get }
        .filter { _.isPublished }
        .flatMap { mad =>
          val futs = MExtServices.values
            .iterator
            .map { _.adMetaTagsRender(mad).map { rl =>
              rl.map { _.render()(ctx) }
                .iterator
            }}
          Future
            .fold[Iterator[Html], Iterator[Html]] (futs) (Iterator.empty) (_ ++ _)
            .map { _.toSeq }
        }
        .recover { case ex: Throwable =>
          if (!ex.isInstanceOf[NoSuchElementException])
            LOGGER.warn("Failed to collect meta-tags for ad " + siteArgs.povAdId, ex)
          Seq.empty[Html]
        }
    }
    // Узел, с которым связан данный запрос.
    val nodeOptFut = MAdnNodeCache.maybeGetByIdCached( siteArgs.adnId )
      .recover { case ex: Throwable =>
        LOGGER.warn("Failed to get node adnId = " + siteArgs.adnId, ex)
        None
      }
    // Сборка результата.
    for {
      _nodeOpt   <- nodeOptFut
      _headAfts  <- headAftersFut
    } yield {
      new ScSiteArgs {
        override def indexCall = routes.MarketShowcase.geoShowcase()
        override def nodeOpt   = _nodeOpt
        override def withGeo   = true   // TODO Чего делает этот параметр? Нужен ли он вообще?
        override val scColors  = super.scColors
        override def headAfter: Traversable[Html] = {
          super.headAfter ++ _headAfts
        }
      }
    }
  }

  /**
   * Раздавалка "сайта" выдачи первой страницы. Можно переопределять, для изменения/расширения функционала.
   * @param siteArgs Доп.аргументы для рендера сайта.
   * @param request Реквест.
   */
  protected def _geoSiteResult(siteArgs: SiteQsArgs)(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    _getSiteRenderArgs(siteArgs) map { args =>
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
      override def indexCall = scCall
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


