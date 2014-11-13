package controllers.sc

import controllers.SioController
import models._
import play.api.mvc.Result
import play.twirl.api.{Html, HtmlFormat}
import util.PlayMacroLogsI
import util.acl.AbstractRequestWithPwOpt
import views.html.market.showcase.demoWebsiteTpl
import play.api.libs.concurrent.Execution.Implicits.defaultContext

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
trait ScSyncSiteGeo extends ScSyncSite with ScSiteGeo with ScIndexGeo with ScAdsTile with ScFocusedAds {

  /**
   * Раздавалка "сайта" выдачи первой страницы. Можно переопределять, для изменения/расширения функционала.
   * @param request Реквест.
   */
  override protected def _geoSiteResult(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    request.ajaxJsScState match {
      case None =>
        super._geoSiteResult
      case Some(jsState) =>
        _syncGeoSite(jsState)
    }
  }

  /**
   * Синхронный рендер выдачи без каких-либо асинхронных участий на основе указанного состояния.
   * @param scState Состояние js-выдачи.
   */
  protected def _syncGeoSite(scState: ScJsState)(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    val logic = new ScSyncSiteLogic {
      override def _scState: ScJsState = scState
      override implicit def _request = request
    }
    logic.resultFut
  }


  /** Логика работы синхронного сайта описывается этим трейтом и связями в нём. */
  trait ScSyncSiteLogic { that =>
    def _scState: ScJsState
    implicit def _request: AbstractRequestWithPwOpt[_]
    
    // Рендерим плитку (findAds)
    def tileLogic = new TileAdsLogic {
      override type T = HtmlFormat.Appendable
      override implicit def _request = that._request

      override val _adSearch = new AdSearch {
        // TODO Прокачать сюда остальные куски состояния, по мере расширения js-состояния.
        override def receiverIds = _scState.adnId.toList
        override def generation = _scState.generationOpt
        override def geo = _scState.geo
      }

      override def renderMadAsync(mad: MAd): Future[T] = {
        Future {
          renderMad2html(mad)
        }
      }
    }

    /** Логика поддержки отображения focused ads, т.е. просматриваемой карточки. */
    def focusedLogic = new FocusedAdsLogic {
      override type OBT = Html
      override implicit def _request = that._request

      /** Рендер заэкранного блока. В случае Html можно просто вызвать renderBlockHtml(). */
      override def renderOuterBlock(madsCountInt: Int, mad: MAd, index: Int, producer: MAdnNode): Future[OBT] = {
        renderBlockHtml(madsCountInt = madsCountInt, mad = mad, index = index, producer = producer)
      }

      override def _withHeadAd: Boolean = true

      override def _adSearch: AdSearch = {
        new AdSearch {
          override def forceFirstIds = _scState.fadsOpenedOpt.toList
          override def maxResultsOpt = Some(1)
          override def generation = _scState.generationOpt
          override def receiverIds = _scState.adnId.toList
          override def offsetOpt = _scState.fadsOffsetOpt
        }
      }
    }

    def maybeFocusedContent: Future[Option[Html]] = {
      if (_scState.fadsOffsetOpt.isDefined) {
         focusedLogic.focAdHtmlOptFut
      } else {
        Future successful None
      }
    }

    def tilesRenderFut = tileLogic.madsRenderedFut

    // Рендерим indexTpl
    /** Готовим контейнер с аргументами рендера indexTpl. */
    def indexRenderArgs = {
      val _tilesRenderFut = tilesRenderFut
      for {
        _focusedContentOpt  <- maybeFocusedContent
        _inlineTiles        <- _tilesRenderFut
      } yield {
        new ScReqArgsDflt {
          // TODO нужно и screen наверное выставлять по-нормальному?
          override def geo = _scState.geo
          override def inlineTiles = _inlineTiles
          override def focusedContent = _focusedContentOpt
        }
      }
    }

    def indexHtmlFut = indexRenderArgs.flatMap(_geoShowCaseHtml)


    // Рендерим site.html, т.е. базовый шаблон выдачи.
    /** Готовим аргументы базового шаблона выдачи. */
    def siteArgsFut: Future[ScSiteArgs] = {
      val _indexHtmlFut = indexHtmlFut
      for {
        siteRenderArgs <- _getSiteRenderArgs
        indexHtml      <- _indexHtmlFut
      } yield {
        new ScSiteArgsWrapper {
          override def _scSiteArgs = siteRenderArgs
          override def inlineIndex = Some(indexHtml)
        }
      }
    }

    def resultFut = siteArgsFut map { args1 =>
      Ok(demoWebsiteTpl(args1))
    }

  }
  
}
