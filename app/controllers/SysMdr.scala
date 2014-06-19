package controllers

import util.acl.IsSuperuser
import scala.concurrent.ExecutionContext.Implicits.global
import util.SiowebEsUtil.client
import util.event.SiowebNotifier.Implicts.sn
import views.html.sys1.mdr._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.06.14 10:45
 * Description: Sys Moderation - контроллер, заправляющий s.io-модерацией рекламных карточек.
 */
object SysMdr extends SioController {

  /** Отобразить начальную страницу раздела модерации рекламных карточек. */
  def index = IsSuperuser.async { implicit request =>
    Ok(mdrIndexTpl())
  }

}
