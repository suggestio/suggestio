package controllers.sc

import controllers.routes
import models._
import models.mctx.Context
import models.msc._
import models.req.IReq
import play.api.mvc._
import play.twirl.api.Html
import util.PlayMacroLogsI
import util.acl._
import util.di.{IScStatUtil, IScUtil}
import util.ext.IExtServicesUtilDi
import views.html.sc._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 19:42
 * Description: Трейты с экшенами для рендера "сайтов" выдачи, т.е. html-страниц, возвращаемых при непоср.реквестах.
 * Бывает рендер через geo, который ищет подходящий узел, и рендер напрямую.
 */

/** Базовый трейт с утилью для сборки конкретных реализация экшенов раздачи "сайтов" выдачи. */
trait ScSiteBase
  extends ScController
  with PlayMacroLogsI
  with IScStatUtil
  with IScUtil
  with IExtServicesUtilDi
{

  import mCommonDi._

  /** Настраиваемая логика сборки результата запроса сайта выдачи. */
  protected abstract class SiteLogic {

    /** Сюда передаются исходные параметры запроса сайта (qs). */
    def _siteQsArgs: SiteQsArgs

    /** Исходный http-реквест. */
    implicit def _request: IReq[_]

    /** Контекст рендера нижелижещих шаблонов. */
    implicit lazy val ctx = implicitly[Context]

    /** Опциональный id текущего узла. */
    def adnIdOpt: Option[String] = _siteQsArgs.adnId

    /** Опциональный экземпляр текущего узла. */
    def nodeOptFut: Future[Option[MNode]] = {
      mNodeCache.maybeGetByIdCached( adnIdOpt )
    }

    /** Добавки к тегу head в siteTpl. */
    def headAfterFut: Future[List[Html]] = {
      mNodeCache.maybeGetByIdCached( _siteQsArgs.povAdId )
        .map { _.get }
        // Интересует только карточка с ресивером. TODO И отмодерированная?
        .filter { mad =>
          mad.edges
            .withPredicateIter(MPredicates.Receiver)
            .exists(_.info.sls.nonEmpty)
        }
        // Зарендерить всё параллельно.
        .flatMap { mad =>
          val futs = for (svcHelper <- extServicesUtil.HELPERS) yield {
            for (renderables <- svcHelper.adMetaTagsRender(mad)) yield {
              for (renderable <- renderables) yield {
                renderable.render()(ctx)
              }
            }
          }
          for (renders <- Future.sequence(futs)) yield {
            renders.iterator
              .flatten
              .toList
          }
        }
        // Отработать случи отсутствия карточки или другие нежелательные варианты.
        .recover { case ex: Throwable =>
          if (!ex.isInstanceOf[NoSuchElementException])
            LOGGER.warn("Failed to collect meta-tags for ad " + _siteQsArgs.povAdId, ex)
          List.empty[Html]
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
          override val scColors = scUtil.siteScColors(nodeOpt)
          override def headAfter: Traversable[Html] = {
            super.headAfter ++ _headAfter
          }
          override def scriptHtml = _scriptHtml
          override def apiVsn = _siteQsArgs.apiVsn
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
      for (render <- renderFut) yield {
        cacheControlShort {
          Ok( render )
        }
      }
    }

  }


  /** Когда нужно рендерить site script, подмешиваем это. */
  protected abstract class SiteScriptLogicV2 extends SiteLogic {

    import views.html.sc.script._

    /** Флаг активности географии. */
    def _withGeo: Boolean
    /** Ссылка на вызов выдачи. */
    def _indexCall: Call

    lazy val scriptRenderArgs: IScScriptRenderArgs = {
      ScScriptRenderArgs(
        withGeo   = _withGeo,
        indexCall = _indexCall,
        adnIdOpt  = adnIdOpt
      )
    }

    /** Добавки к тегу head в siteTpl. */
    override def headAfterFut: Future[List[Html]] = {
      val fut0 = super.headAfterFut
      val headAfterHtml = _headAfterV2Tpl(scriptRenderArgs)(ctx)
      for (htmls0 <- fut0) yield {
        headAfterHtml :: htmls0
      }
    }

    override def scriptHtmlFut: Future[Html] = {
      val html = _scriptV2Tpl(scriptRenderArgs)(ctx)
      Future.successful(html)
    }

  }


}


/** Поддержка гео-сайта. */
trait ScSiteGeo
  extends ScSiteBase
  with IScStatUtil
  with MaybeAuth
{

  import mCommonDi._

  /** Пользователь заходит в sio.market напрямую через интернет, без помощи сторонних узлов. */
  def geoSite(maybeJsState: ScJsState, siteArgs: SiteQsArgs) = MaybeAuth().async { implicit request =>
    if (maybeJsState.nonEmpty) {
      // Было раньше MovedPermanently, но почему-то оно может сбойнуть и закешироваться на CDN.
      // 2016.02.04 Логгирование тут усилено для отлова memleak'а с зацикливанием здесь.
      LOGGER.trace(s"geoSite($siteArgs): Qs js state is nonEmpty, redirecting from ${request.path} [${request.remoteAddress}]")
      val call = routes.Sc.geoSite(x = siteArgs).url + "#!?" + maybeJsState.toQs()
      Redirect(call)
    } else {
      _geoSite(siteArgs)
    }
  }

  /**
   * Тело экшена _geoSite задаётся здесь, чтобы его можно было переопределять при необходимости.
   *
   * @param request Экземпляр реквеста.
   * @return Результат работы экшена.
   */
  protected def _geoSite(siteArgs: SiteQsArgs)(implicit request: IReq[_]): Future[Result] = {
    // Запускаем сбор статистики в фоне.
    _geoSiteStats
    // Запускаем выдачу результата запроса:
    _geoSiteResult(siteArgs)
  }

  /** Фоновый сбор статистики. Можно переназначать. */
  protected def _geoSiteStats(implicit request: IReq[_]): Future[_] = {
    val fut = Future {
      scStatUtil.SiteStat()
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
   *
   * @param siteArgs Доп.аргументы для рендера сайта.
   * @param request Реквест.
   */
  protected def _geoSiteResult(siteArgs: SiteQsArgs)(implicit request: IReq[_]): Future[Result] = {
    val logic = new SiteScriptLogicV2 {
      override implicit def _request  = request
      override def _siteQsArgs          = siteArgs

      override def _withGeo           = siteArgs.adnId.isEmpty
      override def _indexCall         = routes.Sc.geoShowcase()  // TODO Для index call можно какие-то аргументы передать...
    }
    logic.resultFut
  }

}
