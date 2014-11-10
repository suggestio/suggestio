package controllers.sc

import controllers.SioController
import models.JsShowCaseState
import play.api.mvc.Result
import util.PlayMacroLogsI
import util.acl.AbstractRequestWithPwOpt

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.11.14 12:55
 * Description: Синхронная выдача. Т.е. выдача. которая работает без JS на клиенте.
 * Это нужно для для кравлеров и пользователей, у которых JS отключен или проблематичен.
 */
trait ScSyncSite extends SioController with PlayMacroLogsI {


}

trait ScSyncSiteGeo extends ScSyncSite with ScSiteGeo {

  /**
   * Раздавалка "сайта" выдачи первой страницы. Можно переопределять, для изменения/расширения функционала.
   * @param request Реквест.
   */
  override protected def _geoSiteResult(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    request.ajaxJsScState match {
      case None =>
        super._geoSiteResult
      case Some(jsState) =>
        _syncSite(jsState)
    }
  }

  /**
   * Синхронный рендер выдачи без каких-либо асинхронных участий на основе указанного состояния.
   * @param scState Состояние js-выдачи.
   */
  def _syncSite(scState: JsShowCaseState): Future[Result] = {
    // TODO Нужно рендерить indexTpl, вставляя результат в siteTpl().
    // TODO Потом надо рендерить открытую рекламную карточку (параллельно с indexTpl) и вставлять fads container.
    ???
  }

}
