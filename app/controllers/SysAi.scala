package controllers

import util.PlayLazyMacroLogsImpl
import util.acl.IsSuperuser
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import views.html.sys1.ai._, mad._
import models.ai._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.14 18:05
 * Description: Управление системами автоматической генерации контента.
 * На момент создания здесь система заполнения карточек, живущая в MadAiUtil и её модель.
 */
object SysAi extends SioController with PlayLazyMacroLogsImpl {

  import LOGGER._

  /** Раздача страницы с оглавлением по ai-подсистемам. */
  def index = IsSuperuser { implicit request =>
    Ok(indexTpl())
  }

  /** Заглавная страница генераторов рекламных карточек. */
  def madIndex = IsSuperuser.async { implicit request =>
    val aisFut = MAiMad.getAll()
    aisFut map { ais =>
      Ok(madIndexTpl(ais))
    }
  }

}
