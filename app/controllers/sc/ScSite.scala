package controllers.sc

import controllers.{routes, SioController}
import models.mext.MExtServices
import models.msc._
import play.twirl.api.Html
import util.PlayMacroLogsI
import util.showcase._
import util.acl._
import util.xplay.SioHttpErrorHandler
import views.html.sc._
import models._
import scala.concurrent.Future
import play.api.mvc._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 19:42
 * Description: Трейты с экшенами для рендера "сайтов" выдачи, т.е. html-страниц, возвращаемых при непоср.реквестах.
 * Бывает рендер через geo, который ищет подходящий узел, и рендер напрямую.
 */

/** Базовый трейт с утилью для сборки конкретных реализация экшенов раздачи "сайтов" выдачи. */
trait ScSiteBase extends ScController with PlayMacroLogsI {

  /** Настраиваемая логика сборки результата запроса сайта выдачи. */
  protected trait SiteLogic {

    /** Сюда передаются исходные параметры запроса сайта (qs). */
    def _siteArgs: SiteQsArgs

    /** Исходный http-реквест. */
    implicit def _request: AbstractRequestWithPwOpt[_]

    /** Контекст рендера нижелижещих шаблонов. */
    implicit lazy val ctx = implicitly[Context]

    /** Опциональный id текущего узла. */
    def adnIdOpt: Option[String] = _siteArgs.adnId

    /** Опциональный экземпляр текущего узла. */
    def nodeOptFut: Future[Option[MAdnNode]] = {
      MAdnNodeCache.maybeGetByIdCached( adnIdOpt )
    }

    /** Добавки к тегу head в siteTpl. */
    def headAfterFut: Future[Traversable[Html]] = {
      MAd.maybeGetById( _siteArgs.povAdId )
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
            LOGGER.warn("Failed to collect meta-tags for ad " + _siteArgs.povAdId, ex)
          Seq.empty[Html]
        }
    }

    /** Какой скрипт рендерить? */
    def scriptHtmlFut: Future[Html]


    /** Здесь описывается методика сборки аргументов для рендера шаблонов. */
    def renderArgsFut: Future[ScSiteArgs] = {
      val _nodeOptFut     = nodeOptFut
      val _headAfterFut   = headAfterFut
      val _scriptHtmlFut  = scriptHtmlFut
      for {
        _nodeOpt    <- _nodeOptFut
        _headAfter  <- _headAfterFut
        _scriptHtml <- _scriptHtmlFut
      } yield {
        new ScSiteArgs {
          override def nodeOpt = _nodeOpt
          override val scColors = ShowcaseUtil.siteScColors(nodeOpt)
          override def headAfter: Traversable[Html] = {
            super.headAfter ++ _headAfter
          }
          override def scriptHtml = _scriptHtml
          override def apiVsn = _siteArgs.apiVsn
        }
      }
    }

    /** Отрендерить html-тело результата запроса. */
    def renderFut: Future[Html] = {
      for {
        args <- renderArgsFut
      } yield {
        siteTpl(args)(ctx)
      }
    }

    /** Собрать ответ на HTTP-запрос сайта. */
    def resultFut: Future[Result] = {
      renderFut map { render =>
        cacheControlShort {
          Ok( render )
        }
      }
    }

  }


  /** Когда нужно рендерить site script, подмешиваем это. */
  protected trait SiteScript extends SiteLogic {
    /** Флаг активности географии. */
    def _withGeo: Boolean
    /** Ссылка на вызов выдачи. */
    def _indexCall: Call

    /** Какой шаблон скрипта надо рендерить для переданной в _siteArgs версии API? */
    def _scriptTplForApiVsn = _siteArgs.apiVsn.scriptTpl

    def scriptRenderArgsFut: Future[IScScriptRenderArgs] = {
      val res = ScScriptRenderArgs(
        withGeo   = _withGeo,
        indexCall = _indexCall,
        adnIdOpt  = adnIdOpt
      )
      Future successful res
    }

    override def scriptHtmlFut: Future[Html] = {
      val argsFut = scriptRenderArgsFut
      val tpl = _scriptTplForApiVsn
      for {
        args <- argsFut
      } yield {
        tpl.render(args, ctx)
      }
    }

  }


  /** Можно подавлять ошибки чтения экземпляра узла. Возникновении ошибки чтения -- маловероятная ситуация, но экземпляр
    * узла не является для сайта очень обязательным для сайта выдачи.
    * С другой стороны, лучше сразу выдать ошибку юзеру, чем отрендерить зависшую на GET showcaseIndex выдачу. */
  protected trait NodeSuppressErrors extends SiteLogic {
    /** Опциональный экземпляр текущего узла. */
    override def nodeOptFut: Future[Option[MAdnNode]] = {
      super.nodeOptFut
        .recover { case ex: Throwable =>
          LOGGER.warn("Failed to get node adnId = " + _siteArgs.adnId, ex)
          None
        }
    }
  }

}


/** Поддержка гео-сайта. */
trait ScSiteGeo extends ScSiteBase {

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
   * Раздавалка "сайта" выдачи первой страницы. Можно переопределять, для изменения/расширения функционала.
   * @param siteArgs Доп.аргументы для рендера сайта.
   * @param request Реквест.
   */
  protected def _geoSiteResult(siteArgs: SiteQsArgs)(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    val logic = new SiteLogic with SiteScript {
      override implicit def _request  = request
      override def _siteArgs          = siteArgs

      override def _withGeo           = siteArgs.adnId.isEmpty
      override def _indexCall         = routes.MarketShowcase.geoShowcase()  // TODO Для index call можно какие-то аргументы передать...
    }
    logic.resultFut
  }


  /** Раньше выдача пряталась в /market/geo/site. Потом переехала на главную. */
  def rdrToGeoSite = Action { implicit request =>
    val call = routes.MarketShowcase.geoSite().url
    MovedPermanently(call)
  }

}


/** Поддержка node-сайтов. */
trait ScSiteNode extends ScSiteBase {

  /** Экшн, который рендерит страничку с дефолтовой выдачей узла. */
  def demoWebSite(adnId: String) = AdnNodeMaybeAuth(adnId).async { implicit request =>
    val isReceiver = request.adnNode.adn.isReceiver
    val nodeEnabled = request.adnNode.adn.isEnabled
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
      val logic = new SiteLogic with SiteScript {
        override implicit def _request = request
        override val _siteArgs = SiteQsArgs(adnId = Some(adnId))
        override def _withGeo: Boolean = false
        override def _indexCall: Call = routes.MarketShowcase.showcase(adnId)
      }
      logic.resultFut

    } else {
      LOGGER.debug(s"demoWebSite($adnId): Requested node exists, but not available in public: enabled=$nodeEnabled isRcvr=$isReceiver")
      SioHttpErrorHandler.http404ctx
    }
  }

}

