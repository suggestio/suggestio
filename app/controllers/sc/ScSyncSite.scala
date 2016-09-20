package controllers.sc

import java.util.NoSuchElementException

import controllers.{SioController, routes}
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.fut.FutureUtil
import models._
import models.mctx.Context
import models.msc._
import models.req.IReq
import play.api.mvc.Result
import play.twirl.api.{Html, HtmlFormat}
import util.PlayMacroLogsI
import util.acl.MaybeAuth

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.11.14 12:55
 * Description: Синхронная выдача. Т.е. выдача. которая работает без JS на клиенте.
 * Это нужно для для кравлеров и пользователей, у которых JS отключен или проблематичен.
 */

trait ScSyncSite
  extends SioController
  with PlayMacroLogsI
  with ScSiteGeo
  with ScIndexGeo
  with ScAdsTileBase
  with ScFocusedAdsV2
  with ScNodesListBase
  with ScSiteBase
  with MaybeAuth
{

  import mCommonDi._

  /**
   * Раздавалка "сайта" выдачи первой страницы. Можно переопределять, для изменения/расширения функционала.
   * @param request Реквест.
   */
  override protected def _geoSiteResult(siteQsArgs: SiteQsArgs)(implicit request: IReq[_]): Future[Result] = {
    request.ajaxJsScState.fold [Future[Result]] {
      super._geoSiteResult(siteQsArgs)
    } { jsState =>
      _syncGeoSite(jsState, siteQsArgs) { jsSt =>
        routes.Sc.geoSite(x = siteQsArgs).url + "#!?" + jsSt.toQs()
      }
    }
  }

  /** Прямой доступ к синхронному сайту выдачи. */
  def syncGeoSite(scState: ScJsState, siteArgs: SiteQsArgs) = MaybeAuth().async { implicit request =>
    _syncGeoSite(scState, siteArgs) { jsSt =>
      routes.Sc.syncGeoSite(jsSt).url
    }
  }

  /**
   * Синхронный рендер выдачи без каких-либо асинхронных участий на основе указанного состояния.
   * @param scState Состояние js-выдачи.
   * @param urlGenF Генератор внутренних ссылок, получающий на вход изменённое состояние выдачи.
   */
  protected def _syncGeoSite(scState: ScJsState, siteArgs: SiteQsArgs)(urlGenF: ScJsState => String)
                            (implicit request: IReq[_]): Future[Result] = {
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

    implicit def _request: IReq[_]

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
            for (rendered <- renderMad2htmlAsync(brArgs)) yield {
              RenderedAdBlock(brArgs.mad, rendered)
            }
          }
        }

      } else {
        // Нет надобности печатать плитку. Просто генерим заглушку логики рендера плитки:
        val _noMadsFut: Future[Seq[MNode]] = Future.successful( Nil )
        new TileAdsLogic {
          override type T = IRenderedAdBlock
          override implicit def _request = that._request
          override lazy val ctx = that.ctx
          override def _adSearch = new AdSearchImpl {
            override def limitOpt: Option[Int] = Some(0)
          }
          override def renderMadAsync(brArgs: blk.RenderArgs): Future[T] = {
            val ex = new UnsupportedOperationException("Dummy tile ads logic impl.")
            Future.failed(ex)
          }
          override lazy val madsRenderedFut: Future[Seq[T]] = Future.successful(Nil)
          override def madsGroupedFut   = _noMadsFut
          override lazy val madsFut     = _noMadsFut
          override def adsCssExternalFut: Future[Seq[AdCssArgs]] = Future.successful(Nil)
        }
      }
    }

    /** Логика поддержки отображения focused ads, т.е. просматриваемой карточки. */
    def focusedLogic = new FocusedLogicV2 with NoBrAcc {

      override type OBT = Html
      override implicit val _request = that._request
      override def _scStateOpt = Some(_scState)
      override lazy val ctx = that.ctx

      /** Рендер заэкранного блока. В случае Html можно просто вызвать renderBlockHtml(). */
      override def renderOuterBlock(args: AdBodyTplArgs): Future[Html] = {
        renderBlockHtml(args)
      }

      override val _adSearch: FocusedAdsSearchArgs = {
        _scState.focusedAdSearch(
          _maxResultsOpt = Some(1)
        )
      }

      override def focAdsHtmlArgsFut: Future[IFocusedAdsTplArgs] = {
        // Нужно добавить в список аргументов данные по syncUrl.
        for (args0 <- super.focAdsHtmlArgsFut) yield {
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
      FutureUtil.optFut2futOpt(maybeFocusedLogic) { fl =>
        fl.focAdHtmlOptFut
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
        for (renderArgs <- super.renderArgsFut) yield {
          new NodeListRenderArgsWrapper {
            override def _nlraUnderlying = renderArgs
            override def syncUrl(jsState: ScJsState) = _urlGenF(jsState)
          }
        }
      }
    }

    def maybeNodesListHtmlFut: Future[Option[Html]] = {
      if (_scState.isNavScrOpened) {
        // Рендерим, пробрасывая js-состояние внутрь шаблона.
        for (rnl <- nodesListLogic.nodesListRenderedFut) yield {
          Some( rnl(Some(_scState)) )
        }
      } else {
        Future.successful( None )
      }
    }

    def maybeGeoDetectResultFut: Future[Option[GeoDetectResult]] = {
      if (_scState.isNavScrOpened) {
        nodesListLogic.nextNodeWithLayerFut
          .map( EmptyUtil.someF )
      } else {
        Future.successful( None )
      }
    }

    def currNodeGeoOptFut: Future[Option[MNode]] = {
      for (gdrOpt <- maybeGeoDetectResultFut) yield {
        for (gdr <- gdrOpt) yield {
          gdr.node
        }
      }
    }

    def tilesRenderFut = tileLogic.madsRenderedFut

    /** Готовим контейнер с аргументами рендера indexTpl. */
    def indexReqArgsFut: Future[MScIndexArgs] = {
      val r = new MScIndexArgsDfltImpl {
        // TODO нужно и screen наверное выставлять по-нормальному?
        // TODO override def geo                = _scState.geo
      }
      Future.successful(r)
    }

    /** Доп.аргументы для index-выдачи для нужд синхронного рендера. */
    def indexSyncArgsFut: Future[MScIndexSyncArgs] = {
      val _tilesRenderFut = tilesRenderFut
      val _focusedContentOptFut = maybeFocusedContent
      val _maybeNodesListHtmlFut = maybeNodesListHtmlFut
      for {
        _currNodeGeoOpt     <- currNodeGeoOptFut
        _focusedContentOpt  <- _focusedContentOptFut
        _inlineTiles        <- _tilesRenderFut
        _nodesListHtmlOpt   <- _maybeNodesListHtmlFut
      } yield {
        new MScIndexSyncArgs {
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
      val adnIdOpt = _scState.adnId
        .orElse(_siteArgs.adnId)
      mNodeCache.maybeGetByIdCached( adnIdOpt )
    }


    /** Реализация GeoIndexLogic для нужд ScSyncSite.
      * Рендер результата идёт в Html. */
    trait HtmlGeoIndexLogic extends GeoIndexLogic {
      override type T = Html

      private def helper2respHtml(h: Future[ScIndexHelperBase]): Future[T] = {
        h.flatMap(_.respHtmlFut)
      }

      /** Нет ноды. */
      override def nodeNotDetected(): Future[T] = {
        helper2respHtml(
          nodeNotDetectedHelperFut()
        )
      }

      /** Нода найдена с помощью геолокации. */
      override def nodeDetected(gdr: GeoDetectResult): Future[T] = {
        helper2respHtml(
          nodeFoundHelperFut(gdr)
        )
      }
    }


    def indexHtmlLogicFut: Future[HtmlGeoIndexLogic] = {
      val _indexSyncArgsFut = indexSyncArgsFut
      for {
        indexReqArgs  <- indexReqArgsFut
        indexSyncArgs <- _indexSyncArgsFut
      } yield {
        new HtmlGeoIndexLogic {
          override def _reqArgs  = indexReqArgs
          override def _syncArgs = indexSyncArgs
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
                Future.successful( gdr )

              case None =>
                // Имитируем экзепшен, чтобы перехватить его в Future.recover():
                val ex = new NoSuchElementException("Receiver node not exists or undefined: " + _scState.adnId)
                Future.failed(ex)
            }
            // Если нет возможности использовать заданный узел, пытаемся определить через метод супер-класса.
            .recoverWith {
              case ex: Exception =>
                if (!ex.isInstanceOf[NoSuchElementException])
                  LOGGER.error("Unable to make node search. nodeIdOpt = " + _scState.adnId, ex)
                super._gdrFut
            }
          }

          override def nodeFoundHelperFut(gdr: GeoDetectResult): Future[ScIndexNodeGeoHelper] = {
            val helper = new ScIndexNodeGeoHelper with ScIndexHelperAddon {
              override def _syncArgs = indexSyncArgs
              override val gdrFut = Future.successful( gdr )
              override def welcomeAdOptFut = Future.successful( None )
            }
            Future.successful( helper )
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
    def adsCssExtFut: Future[Option[Html]] = {
      for {
        mls <- Future.traverse(madsLogics) { ml =>
          ml.adsCssExternalFut
        }
      } yield {
        if (mls.isEmpty) {
          None
        } else {
          val args = mls.flatten
          val html = htmlAdsCssLink(args)(ctx)
          Some(html)
        }
      }
    }

    /** Реализация [[SiteLogic]] для нужд [[ScSyncSite]]. */
    protected class SyncSiteLogic extends SiteLogic {
      // Линкуем исходные данные логики с полями outer-класса.
      override implicit lazy val ctx  = that.ctx
      override def _siteQsArgs        = that._siteArgs
      override implicit def _request  = that._request
      override def nodeOptFut         = that.adnNodeReqFut
      override def _syncRender        = true
      /** Не нужно передавать в siteTpl никаких данных состояния sc-sjs, т.к. мы без JS работаем. */
      override def customScStateOptFut = Future.successful(None)

      // TODO Этот код метода был написан спустя много времени после остальной реализации. Нужно протестить всё.
      override def headAfterFut: Future[List[Html]] = {
        val supFut = super.headAfterFut
        for (headAfterOpt <- that.adsCssExtFut;  headAfter0 <- supFut) yield {
          headAfterOpt.fold(headAfter0)(_ :: headAfter0)
        }
      }

      /** Скрипт выдачи не нужен вообще. */
      override def scriptHtmlFut: Future[Html] = {
        Future.successful(HtmlFormat.empty)
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
