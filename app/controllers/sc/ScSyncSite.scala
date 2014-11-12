package controllers.sc

import controllers.SioController
import models._
import play.api.mvc.Result
import play.twirl.api.HtmlFormat
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
trait ScSyncSiteGeo extends ScSyncSite with ScSiteGeo with ScIndexGeo with ScAdsTile {

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
    def tileLogic = {
      new TileAdsLogic {
        override type T = HtmlFormat.Appendable
        override implicit def _request = that._request

        override val _adSearch = AdSearch(
          // TODO Прокачать сюда остальные куски состояния, по мере расширения js-состояния.
          receiverIds = _scState.adnId.toList,
          generation = _scState.generationOpt,
          geo = _scState.geo
        )

        override def renderMadAsync(mad: MAd): Future[T] = {
          Future {
            renderMad2html(mad)
          }
        }
      }
    }
    
    def tilesRenderFut = tileLogic.madsRenderedFut
    
    // Рендерим indexTpl.
    // TODO рендерить открытую рекламную карточку (focusedAds) и вставлять в fads container.
    def indexRenderArgs = {
      for {
        _inlineTiles <- tilesRenderFut
      } yield {
        new ScReqArgsDflt {
          // TODO нужно и screen наверное выставлять по-нормальному?
          override def geo = _scState.geo
          override def inlineTiles = _inlineTiles
        }
      }
    }
    
    def indexHtmlFut = indexRenderArgs.flatMap(_geoShowCaseHtml)
    
    // Рендерим site.html, т.е. базовый шаблон выдачи.
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
