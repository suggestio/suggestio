package controllers

import models.event.MEvent
import util.PlayMacroLogsImpl
import util.acl.IsAdnNodeAdmin
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.01.15 11:08
 * Description: Контроллер раздела уведомлений в личном кабинете.
 * Контроллер поддерживает отображение уведомлений, удаление оных и прочие действия.
 */
object LkEvents extends SioControllerImpl with PlayMacroLogsImpl {

  /**
   * Рендер страницы текущих нотификаций.
   * @param adnId id узла.
   * @return 200 OK + страница со списком уведомлений.
   */
  def nodeIndex(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    val eventsFut = MEvent.findByOwner(adnId, limit = 100)
    ???
  }

}
