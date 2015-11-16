package controllers.sc

import java.util.NoSuchElementException

import controllers.{routes, SioController}
import models._
import models.msc._
import play.api.mvc.Result
import play.twirl.api.{HtmlFormat, Html}
import util.PlayMacroLogsI
import util.acl.{MaybeAuth, AbstractRequestWithPwOpt}
import util.di.INodeCache

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.11.14 12:55
 * Description: Синхронная выдача. Т.е. выдача. которая работает без JS на клиенте.
 * Это нужно для для кравлеров и пользователей, у которых JS отключен или проблематичен.
 */
trait ScSyncSite extends SioController with PlayMacroLogsI


/** Аддон для контроллера, добавляет поддержку синхронного гео-сайта выдачи. */
trait ScSyncSiteGeo
  extends ScSyncSite
  with ScSiteGeo
  with ScIndexGeo
  with ScAdsTileBase
  with ScFocusedAdsBase
  with ScNodesListBase
  with ScSiteBase
  with MaybeAuth
  with INodeCache
{

  /**
   * Раздавалка "сайта" выдачи первой страницы. Можно переопределять, для изменения/расширения функционала.
   * @param request Реквест.
   */
  override protected def _geoSiteResult(siteArgs: SiteQsArgs)(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    request.ajaxJsScState match {
      case None =>
        super._geoSiteResult(siteArgs)
      case Some(jsState) =>
        _syncGeoSite(jsState, siteArgs) { jsSt =>
          routes.MarketShowcase.geoSite(x = siteArgs).url + "#!?" + jsSt.toQs()
        }
    }
  }

  /** Прямой доступ к синхронному сайту выдачи. */
  def syncGeoSite(scState: ScJsState, siteArgs: SiteQsArgs) = MaybeAuth.async { implicit request =>
    _syncGeoSite(scState, siteArgs) { jsSt =>
      routes.MarketShowcase.syncGeoSite(jsSt).url
    }
  }

  /**
   * Синхронный рендер выдачи без каких-либо асинхронных участий на основе указанного состояния.
   * @param scState Состояние js-выдачи.
   * @param urlGenF Генератор внутренних ссылок, получающий на вход изменённое состояние выдачи.
   */
  protected def _syncGeoSite(scState: ScJsState, siteArgs: SiteQsArgs)(urlGenF: ScJsState => String)
                            (implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    val logic = new ScSyncSiteLogic {
      override def _scState   = scState
      override def _urlGenF   = urlGenF
      override def _siteArgs  = siteArgs
      override implicit def _request  = request
    }
    logic.resultFut
  }



  /** Логика работы синхронного сайта описывается этим трейтом и связями в нём. */
  trait ScSyncSiteLogic { that =>
    /** Состояние выдачи. */
    def _scState: ScJsState

    /** Дополнительные параметры для сайта. */
    def _siteArgs: SiteQsArgs

    implicit def _request: AbstractRequestWithPwOpt[_]

    /** Генератор ссылок на выдачу. */
    def _urlGenF: ScJsState => String

    /** Контекст рендера шаблонов. */
    lazy val ctx = implicitly[Context]

    /** Есть ли какая-либо необходимость в рендере плитки карточек? */
    def needRenderTiles: Boolean = {
      _scState.fadOpenedIdOpt.isEmpty
    }

    // TODO В logic'ах наверное надо дедублицировать lazy val ctx.

    /** Подготовка к рендеру плитки (findAds). Наличие focusedAds перекрывает это, поэтому тут есть выбор. */
    lazy val tileLogic: TileAdsLogic { type T = IRenderedAdBlock } = {
      if (needRenderTiles) {
        // Рендерим плитку, как этого требует needRenderTiles().
        new TileAdsLogic {
          override type T = IRenderedAdBlock
          override implicit def _request = that._request
          override val _adSearch = _scState.tilesAdSearch()
          override lazy val ctx = that.ctx
          override def renderMadAsync(brArgs: blk.RenderArgs): Future[T] = {
            renderMad2htmlAsync(brArgs) map { rendered =>
              RenderedAdBlock(brArgs.mad, rendered)
            }
          }
        }

      } else {
        // Нет надобности печатать плитку. Просто генерим заглушку логики рендера плитки:
        val _noMadsFut: Future[Seq[MNode]] = Future successful Nil
        new TileAdsLogic {
          override type T = IRenderedAdBlock
          override implicit def _request = that._request
          override lazy val ctx = that.ctx
          override def _adSearch = new AdSearchImpl {
            override def limitOpt: Option[Int] = Some(0)
          }
          override def renderMadAsync(brArgs: blk.RenderArgs): Future[T] = {
            Future failed new UnsupportedOperationException("Dummy tile ads logic impl.")
          }
          override lazy val madsRenderedFut: Future[Seq[T]] = Future successful Nil
          override def madsGroupedFut   = _noMadsFut
          override lazy val madsFut     = _noMadsFut
          override def adsCssExternalFut: Future[Seq[AdCssArgs]] = Future successful Nil
        }
      }
    }

    /** Логика поддержки отображения focused ads, т.е. просматриваемой карточки. */
    def focusedLogic = new FocusedAdsLogic with NoBrAcc {
      override type OBT = Html
      override implicit def _request = that._request
      override def _scStateOpt = Some(_scState)
      override def apiVsn = MScApiVsns.Coffee   // TODO Версия скопирована, возможно можно/нужно взять версию 2 тут.
      override lazy val ctx = that.ctx

      /** Рендер заэкранного блока. В случае Html можно просто вызвать renderBlockHtml(). */
      override def renderOuterBlock(args: AdBodyTplArgs): Future[OBT] = {
        renderBlockHtml(args)
      }

      override val _adSearch = {
        _scState.focusedAdSearch(
          _maxResultsOpt = Some(1)
        )
      }
      override def focAdsHtmlArgsFut: Future[IFocusedAdsTplArgs] = {
        // Нужно добавить в список аргументов данные по syncUrl.
        super.focAdsHtmlArgsFut map { args0 =>
          new IFocusedAdsTplArgsWrapper {
            override def _underlying: IFocusedAdsTplArgs = args0
            override def syncUrl(jsState: ScJsState) = _urlGenF(jsState)
          }
        }
      }
    }

    lazy val maybeFocusedLogic: Option[FocusedAdsLogic { type OBT = Html }] = {
      if (_scState.fadOpenedIdOpt.nonEmpty) {
        Some(focusedLogic)
      } else {
        None
      }
    }

    def maybeFocusedContent: Future[Option[Html]] = {
      maybeFocusedLogic match {
        case Some(fl) =>
          fl.focAdHtmlOptFut
        case None =>
          Future successful None
      }
    }

    /** Логика отработки списка узлов (панели навигации). */
    lazy val nodesListLogic = new FindNodesLogic {
      override implicit def _request = that._request
      override val _nsArgs = MScNodeSearchArgs(
        currAdnId = _scState.adnId,
        geoMode = _scState.geo
      )
      override def renderArgsFut: Future[NodeListRenderArgs] = {
        super.renderArgsFut map { renderArgs =>
          new NodeListRenderArgsWrapper {
            override def _nlraUnderlying = renderArgs
            override def syncUrl(jsState: ScJsState) = _urlGenF(jsState)
          }
        }
      }
    }

    def maybeNodesListHtmlFut: Future[Option[Html]] = {
      if (_scState.isNavScrOpened) {
        nodesListLogic.nodesListRenderedFut
          .map { rnl => Some(rnl(Some(_scState))) }   // Рендерим, пробрасывая js-состояние внутрь шаблона.
      } else {
        Future successful None
      }
    }

    def maybeGeoDetectResultFut: Future[Option[GeoDetectResult]] = {
      if (_scState.isNavScrOpened) {
        nodesListLogic.nextNodeWithLayerFut
          .map(Some.apply)
      } else {
        Future successful None
      }
    }

    def currNodeGeoOptFut: Future[Option[MNode]] = {
      maybeGeoDetectResultFut.map {
        _.map { gdr =>
          gdr.node
        }
      }
    }

    def tilesRenderFut = tileLogic.madsRenderedFut

    /** Готовим контейнер с аргументами рендера indexTpl. */
    def indexReqArgsFut: Future[ScReqArgs] = {
      val _tilesRenderFut = tilesRenderFut
      val _focusedContentOptFut = maybeFocusedContent
      val _maybeNodesListHtmlFut = maybeNodesListHtmlFut
      for {
        _currNodeGeoOpt     <- currNodeGeoOptFut
        _focusedContentOpt  <- _focusedContentOptFut
        _inlineTiles        <- _tilesRenderFut
        _nodesListHtmlOpt   <- _maybeNodesListHtmlFut
      } yield {
        new ScReqArgsDflt {
          // TODO нужно и screen наверное выставлять по-нормальному?
          override def geo                = _scState.geo
          override def inlineTiles        = _inlineTiles
          override def focusedContent     = _focusedContentOpt
          override def inlineNodesList    = _nodesListHtmlOpt
          override def adnNodeCurrentGeo  = _currNodeGeoOpt
          override def jsStateOpt         = Some(_scState)
          override def syncUrl(jsState: ScJsState) = that._urlGenF(jsState)
        }
      }
    }

    /** Узел, запрошенный в qs-аргументах. */
    lazy val adnNodeReqFut: Future[Option[MNode]] = {
      // Использовать любой заданный id узла, если возможно.
      val adnIdOpt = _scState.adnId orElse _siteArgs.adnId
      mNodeCache.maybeGetByIdCached( adnIdOpt )
    }

    def indexHtmlLogicFut: Future[HtmlGeoIndexLogic] = {
      indexReqArgsFut.map { indexReqArgs =>
        new HtmlGeoIndexLogic {
          override def _reqArgs: ScReqArgs = indexReqArgs
          override implicit def _request = that._request

          /** Определение текущего узла выдачи. Текущий узел может быть задан через параметр ресивера. */
          override def _gdrFut: Future[GeoDetectResult] = {
            adnNodeReqFut.flatMap {
              case Some(node) =>
                // Нужно привести найденный узел к GeoDetectResult:
                val ast = node.extras.adn
                  .flatMap( _.shownTypeIdOpt )
                  .flatMap( AdnShownTypes.maybeWithName )
                  .getOrElse( AdnShownTypes.default )
                val gdr = GeoDetectResult(ast.ngls.head, node)
                Future successful gdr
              case None =>
                // Имитируем экзепшен, чтобы перехватить его в Future.recover():
                Future failed new NoSuchElementException("Receiver node not exists or undefined: " + _scState.adnId)
            }
            // Если нет возможности использовать заданный узел, пытаемся определить через метод супер-класса.
            .recoverWith {
              case ex: Exception =>
                if (!ex.isInstanceOf[NoSuchElementException])
                  LOGGER.error("Unable to make node search. nodeIdOpt = " + _scState.adnId, ex)
                super._gdrFut
            }
          }

          override def nodeFoundHelperFut(gdr: GeoDetectResult): Future[NfHelper_t] = {
            val helper = new ScIndexNodeGeoHelper with ScIndexHelperAddon {
              override val gdrFut = Future successful gdr
              override def welcomeAdOptFut = Future successful None
            }
            Future successful helper
          }
        }
      }
    }

    def indexHtmlFut: Future[Html] = {
      indexHtmlLogicFut.flatMap(_.apply())
    }

    /** Логики, которые относятся к генерирующим карточки рекламные. */
    def madsLogics: Seq[AdCssRenderArgs] = Seq(tileLogic) ++ maybeFocusedLogic

    /** Рендер илайнового css'а блоков, если возможно. */
    def adsCssExtFut: Future[Seq[Html]] = {
      Future.traverse(madsLogics) { ml =>
        ml.adsCssExternalFut
      } map { mls =>
        if (mls.isEmpty) {
          Nil
        } else {
          val args = mls.flatten
          val html = htmlAdsCssLink(args)(ctx)
          Seq(html)
        }
      }
    }

    /** Реализация [[SiteLogic]] для нужд [[ScSyncSite]]. */
    protected class SyncSiteLogic extends SiteLogic {
      // Линкуем исходные данные логики с полями outer-класса.
      override implicit lazy val ctx  = that.ctx
      override def _siteArgs          = that._siteArgs
      override implicit def _request  = that._request
      override def nodeOptFut         = that.adnNodeReqFut

      // TODO Этот код метода был написан спустя много времени после остальной реализации. Нужно протестить всё.
      override def headAfterFut: Future[Traversable[Html]] = {
        val supFut = super.headAfterFut
        for (headAfter <- that.adsCssExtFut;  headAfter0 <- supFut) yield {
          headAfter ++ headAfter0
        }
      }

      /** Скрипт выдачи не нужен вообще. */
      override def scriptHtmlFut: Future[Html] = {
        Future successful HtmlFormat.empty
      }

      /** Подмешиваем необходимые для sync-render данные в аргументы рендера сайта. */
      override def renderArgsFut: Future[ScSiteArgs] = {
        val _indexHtmlFut = indexHtmlFut
        for {
          siteRenderArgs <- super.renderArgsFut
          indexHtml      <- _indexHtmlFut
        } yield {
          new ScSiteArgsWrapper {
            override def _scSiteArgs  = siteRenderArgs
            override def inlineIndex  = Some(indexHtml)
            override def syncRender   = true
          }
        }
      }
    }

    /** Рендерим site.html, т.е. базовый шаблон выдачи. */
    def resultFut: Future[Result] = {
      val logic = new SyncSiteLogic
      logic.resultFut
    }

  }
  
}
