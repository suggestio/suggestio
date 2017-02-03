package controllers.sc

import java.util.NoSuchElementException

import controllers.{SioController, routes}
import io.suggest.common.empty.EmptyUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.model.es.MEsUuId
import models._
import models.mlu.MLookupModes
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
  with ScIndex
  with ScAdsTileBase
  with ScFocusedAdsV2
  with ScNodesListBase
  with ScSiteBase
  with MaybeAuth
{

  import mCommonDi._

  /**
   * Раздавалка "сайта" выдачи первой страницы. Можно переопределять, для изменения/расширения функционала.
   */
  override protected def _geoSiteResult(logic: SiteScriptLogicV2): Future[Result] = {
    logic._request.ajaxJsScState.fold [Future[Result]] {
      super._geoSiteResult(logic)
    } { jsState =>
      _syncGeoSite(jsState, logic._siteQsArgs) { jsSt =>
        routes.Sc.geoSite(x = logic._siteQsArgs).url + "#!?" + jsSt.toQs()
      }(logic._request)
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
      override def _request   = request
    }
    logic.resultFut
  }



  /** Логика работы синхронного сайта описывается этим трейтом и связями в нём. */
  trait ScSyncSiteLogic extends LazyContext { syncLogic =>

    /** Состояние выдачи. */
    def _scState: ScJsState

    /** Дополнительные параметры для сайта. */
    def _siteArgs: SiteQsArgs

    implicit def _request: IReq[_]

    /** Генератор ссылок на выдачу. */
    def _urlGenF: ScJsState => String

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
          override implicit def _request = syncLogic._request
          override val _qs: MScAdsTileQs = {
            MScAdsTileQs(
              search = MScAdsSearchQs(
                genOpt    = _scState.generationOpt,
                rcvrIdOpt = _scState.adnId.map(MEsUuId.apply)
              )
            )
          }
          override lazy val ctx = syncLogic.ctx
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
          override implicit def _request = syncLogic._request
          override lazy val ctx = syncLogic.ctx
          override val _qs: MScAdsTileQs = {
            MScAdsTileQs(
              search = MScAdsSearchQs(
                limitOpt = Some(0)
              )
            )
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
    def focusedLogic = new FocusedLogicV2 {

      override type OBT = Html
      override implicit val _request = syncLogic._request
      override def _scStateOpt = Some(_scState)
      override lazy val ctx = syncLogic.ctx

      /** Рендер заэкранного блока. В случае Html можно просто вызвать renderBlockHtml(). */
      override def renderOuterBlock(args: AdBodyTplArgs): Future[Html] = {
        renderBlockHtml(args)
      }

      override val _qs: MScAdsFocQs = {
        MScAdsFocQs(
          search = MScAdsSearchQs(
            offsetOpt = _scState.fadsOffsetOpt,
            limitOpt  = Some(1),
            rcvrIdOpt = _scState.adnId.map(MEsUuId.apply),
            prodIdOpt = _scState.fadsProdIdOpt.map(MEsUuId.apply),
            genOpt    = _scState.generationOpt
          ),
          lookupMode  = MLookupModes.Around,
          lookupAdId  = _scState.fadOpenedIdOpt.get,
          focJumpAllowed = false,
          screen      = None
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
      override implicit def _request = syncLogic._request
      override val _nsArgs = MScNodeSearchArgs(
        currAdnId = _scState.adnId
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
          override def syncUrl(jsState: ScJsState) = syncLogic._urlGenF(jsState)
        }
      }
    }

    /** Узел, запрошенный в qs-аргументах. */
    lazy val adnNodeReqFut: Future[Option[MNode]] = {
      // Использовать любой заданный id узла, если возможно.
      val adnIdOpt = _scState.adnId
        .orElse(_siteArgs.adnId)
      mNodesCache.maybeGetByIdCached( adnIdOpt )
    }


    def indexHtmlLogicFut: Future[ScIndexUniLogic] = {
      val _indexSyncArgsFut = indexSyncArgsFut
      for {
        indexReqArgs  <- indexReqArgsFut
        indexSyncArgs <- _indexSyncArgsFut
      } yield {
        new ScIndexUniLogicImpl {
          override def _reqArgs   = indexReqArgs
          override def _syncArgs  = indexSyncArgs
          override def _request   = syncLogic._request
          override lazy val ctx   = syncLogic.ctx

          /** Пытаемся задействовать уже имеющийся узел. */
          override def indexNodeFut: Future[MIndexNodeInfo] = {
            val okFut = for (mnodeOpt <- syncLogic.adnNodeReqFut) yield {
              val mnode = mnodeOpt.get
              MIndexNodeInfo(
                mnode   = mnode,
                isRcvr  = true
              )
            }
            okFut.recoverWith { case ex: Throwable =>
              val logPrefix = "sync.indexHtmlLogicFut.indexNodeFut:"
              if (!ex.isInstanceOf[NoSuchElementException])
                LOGGER.error(s"$logPrefix Unable to make sync node search. nodeIdOpt = " + _scState.adnId, ex)
              else
                LOGGER.trace(s"$logPrefix No sync node exists outside the logic.")
              super.indexNodeFut
            }
          }

          /** ip-геолокация кравлера не имеет никакого смысла. */
          override def reqGeoLocFut = Future.successful(None)

          /** Получение карточки приветствия не нужно, т.к. кравлер не требуется приветствовать,
            * да её потом нечем скрывать с экрана: js не работает же. */
          override def welcomeOptFut: Future[Option[WelcomeRenderArgsT]] = Future.successful(None)
        }
      }
    }

    def indexHtmlFut: Future[Html] = {
      indexHtmlLogicFut
        .flatMap(_.respHtmlFut)
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
      override implicit lazy val ctx  = syncLogic.ctx
      override def _siteQsArgs        = syncLogic._siteArgs
      override implicit def _request  = syncLogic._request
      override def nodeOptFut         = syncLogic.adnNodeReqFut
      override def _syncRender        = true
      /** Не нужно передавать в siteTpl никаких данных состояния sc-sjs, т.к. мы без JS работаем. */
      override def customScStateOptFut = Future.successful(None)

      // TODO Этот код метода был написан спустя много времени после остальной реализации. Нужно протестить всё.
      override def headAfterFut: Future[List[Html]] = {
        val supFut = super.headAfterFut
        for (headAfterOpt <- syncLogic.adsCssExtFut;  headAfter0 <- supFut) yield {
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
