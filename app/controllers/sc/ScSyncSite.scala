package controllers.sc

import java.util.NoSuchElementException

import controllers.SioController
import models._
import play.api.mvc.Result
import play.twirl.api.Html
import util.PlayMacroLogsI
import util.acl.{MaybeAuth, AbstractRequestWithPwOpt}
import views.html.market.showcase.demoWebsiteTpl
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client

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
trait ScSyncSiteGeo extends ScSyncSite with ScSiteGeo with ScIndexGeo with ScAdsTile with ScFocusedAds with ScNodesList {

  /**
   * Раздавалка "сайта" выдачи первой страницы. Можно переопределять, для изменения/расширения функционала.
   * @param request Реквест.
   */
  override protected def _geoSiteResult(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    request.ajaxJsScState match {
      case None =>
        super._geoSiteResult
      case Some(jsState) =>
        val qsb = ScJsState.qsbStandalone
        _syncGeoSite(jsState, _.ajaxStatedUrl(qsb))
    }
  }

  /** Прямой доступ к синхронному сайту выдачи. */
  def syncGeoSite(scState: ScJsState) = MaybeAuth.async { implicit request =>
    _syncGeoSite(scState, _.syncSiteUrl)
  }

  /**
   * Синхронный рендер выдачи без каких-либо асинхронных участий на основе указанного состояния.
   * @param scState Состояние js-выдачи.
   * @param urlGenF Генератор внутренних ссылок, получающий на вход изменённое состояние выдачи.
   */
  protected def _syncGeoSite(scState: ScJsState, urlGenF: ScJsState => String)
                            (implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    val logic = new ScSyncSiteLogic {
      override def _scState = scState
      override implicit def _request = request
      override def _urlGenF = urlGenF
    }
    logic.resultFut
  }



  /** Логика работы синхронного сайта описывается этим трейтом и связями в нём. */
  trait ScSyncSiteLogic { that =>
    def _scState: ScJsState
    implicit def _request: AbstractRequestWithPwOpt[_]
    /** Генератор ссылок на выдачу. */
    def _urlGenF: ScJsState => String
    lazy val ctx = implicitly[Context]

    /** Есть ли какая-либо необходимость в рендере плитки карточек? */
    def needRenderTiles: Boolean = {
      _scState.fadOpenedIdOpt.isEmpty
    }

    // TODO В logic'ах наверное надо дедублицировать lazy val ctx.

    /** Подготовка к рендеру плитки (findAds). Наличие focusedAds перекрывает это, поэтому тут есть выбор. */
    lazy val tileLogic: TileAdsLogic { type T = RenderedAdBlock } = {
      if (needRenderTiles) {
        // Рендерим плитку, как этого требует needRenderTiles().
        new TileAdsLogic {
          override type T = RenderedAdBlock
          override implicit def _request = that._request
          override val _adSearch = _scState.tilesAdSearch()
          override lazy val ctx = that.ctx
          override def renderMadAsync(mad: MAd): Future[T] = {
            Future {
              renderMad2html(mad)
            } map { rendered =>
              RenderedAdBlockImpl(mad, rendered)
            }
          }
        }

      } else {
        // Нет надобности печатать плитку. Просто генерим заглушку логики рендера плитки:
        new TileAdsLogic {
          override type T = RenderedAdBlock
          override implicit def _request = that._request
          override lazy val ctx = that.ctx
          override def _adSearch = new AdSearch {
            override def maxResultsOpt: Option[Int] = Some(0)
          }
          override def renderMadAsync(mad: MAd): Future[T] = Future failed new UnsupportedOperationException("Dummy tile ads logic impl.")
          override lazy val madsRenderedFut: Future[Seq[T]] = Future successful Nil
          override lazy val madsGroupedFut: Future[Seq[MAd]] = Future successful Nil
          override lazy val madsFut: Future[Seq[MAd]] = Future successful Nil
          override def adsCssExternalFut: Future[Seq[AdCssArgs]] = Future successful Nil
        }
      }
    }

    /** Логика поддержки отображения focused ads, т.е. просматриваемой карточки. */
    def focusedLogic = new FocusedAdsLogic {
      override type OBT = Html
      override implicit def _request = that._request
      override def _scStateOpt = Some(_scState)
      override lazy val ctx = that.ctx

      /** Рендер заэкранного блока. В случае Html можно просто вызвать renderBlockHtml(). */
      override def renderOuterBlock(madsCountInt: Int, madAndArgs: AdAndBrArgs, index: Int, producer: MAdnNode): Future[OBT] = {
        renderBlockHtml(madsCountInt = madsCountInt, madAndArgs = madAndArgs, index = index, producer = producer)
      }

      override def _withHeadAd: Boolean = true
      override val _adSearch = _scState.focusedAdSearch(
        _maxResultsOpt = Some(1)
      )
      override def focAdsHtmlArgsFut: Future[FocusedAdsTplArgs] = {
        // Нужно добавить в список аргументов данные по syncUrl.
        super.focAdsHtmlArgsFut map { args0 =>
          new FocusedAdsTplArgsWrapper {
            override def _focArgsUnderlying: FocusedAdsTplArgs = args0
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
      override val _nsArgs = SimpleNodesSearchArgs(
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

    def currNodeGeoOptFut: Future[Option[MAdnNode]] = {
      maybeGeoDetectResultFut.map {
        _.map { gdr =>
          gdr.node
        }
      }
    }

    def tilesRenderFut = tileLogic.madsRenderedFut

    // Рендерим indexTpl
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
          override def geo = _scState.geo
          override def inlineTiles = _inlineTiles
          override def focusedContent = _focusedContentOpt
          override def inlineNodesList = _nodesListHtmlOpt
          override def adnNodeCurrentGeo = _currNodeGeoOpt
          override def jsStateOpt = Some(_scState)
          override def syncUrl(jsState: ScJsState) = that._urlGenF(jsState)
        }
      }
    }

    /** Узел, запрошенный в qs-аргументах. */
    lazy val adnNodeReqFut: Future[Option[MAdnNode]] = {
      MAdnNodeCache.maybeGetByIdCached( _scState.adnId )
    }

    def indexHtmlLogicFut: Future[HtmlGeoIndexLogic] = {
      indexReqArgsFut.map { indexReqArgs =>
        new HtmlGeoIndexLogic {
          override def _reqArgs: ScReqArgs = indexReqArgs
          override implicit def _request = that._request

          /** Определение текущего узла выдачи. Текущий узел может быть задан через параметр ресивера. */
          override def gdrFut: Future[GeoDetectResult] = {
            adnNodeReqFut.flatMap {
              case Some(node) =>
                // Нужно привести найденный узел к GeoDetectResult:
                val ngl: AdnShownType = AdnShownTypes.withName( node.adn.shownTypeId )
                val gdr = GeoDetectResult(ngl.ngls.head, node)
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
                super.gdrFut
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

    def headAfterFut: Future[Option[Html]] = {
      Future.traverse(madsLogics) { ml =>
        ml.adsCssExternalFut
      } map { mls =>
        if (mls.isEmpty) {
          None
        } else {
          val args = mls.flatten
          val html = htmlAdsCssLink(args)(ctx)
          Some(html)
        }
      }
    }

    // Рендерим site.html, т.е. базовый шаблон выдачи.
    /** Готовим аргументы базового шаблона выдачи. */
    def siteArgsFut: Future[ScSiteArgs] = {
      val _indexHtmlFut = indexHtmlFut
      val _headAfterFut = headAfterFut
      val _adnNodeReqFut = adnNodeReqFut
      for {
        siteRenderArgs <- _getSiteRenderArgs
        indexHtml      <- _indexHtmlFut
        _headAfter     <- _headAfterFut
        _adnNodeOpt    <- _adnNodeReqFut
      } yield {
        new ScSiteArgsWrapper {
          override def _scSiteArgs  = siteRenderArgs
          override def inlineIndex  = Some(indexHtml)
          override def syncRender   = true
          override def headAfter    = _headAfter
          override def nodeOpt      = _adnNodeOpt
        }
      }
    }

    def resultFut = siteArgsFut map { args1 =>
      Ok(demoWebsiteTpl(args1))
    }

  }
  
}
