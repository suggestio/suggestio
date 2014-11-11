package controllers.sc

import controllers.SioController
import models.{ScReqArgsDflt, ScJsState}
import play.api.mvc.Result
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
trait ScSyncSiteGeo extends ScSyncSite with ScSiteGeo with ScIndexGeo {

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
    // TODO рендерить плитку синхронно (findAds)
    // TODO рендерить открытую рекламную карточку (focusedAds) и вставлять в fads container.
    val indexRenderArgs = new ScReqArgsDflt {
      // TODO нужно и screen наверное выставлять по-нормальному?
      override def geo = scState.geo
    }
    val indexHtmlFut = _geoShowCaseHtml(indexRenderArgs)
    val args1Fut = for {
      siteRenderArgs <- _getSiteRenderArgs
      indexHtml      <- indexHtmlFut
    } yield {
      siteRenderArgs.copy(
        inlineIndex = Some(indexHtml)
      )
    }
    args1Fut map { args1 =>
      Ok(demoWebsiteTpl(args1))
    }
  }

}
