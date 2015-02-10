package controllers

import models.usr.EmailActivation
import util.PlayMacroLogsImpl
import util.acl.IsSuperuser
import views.html.ident.reg.email.emailRegMsgTpl
import views.html.ident.recover.emailPwRecoverTpl
import views.html.sys1.person._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.02.15 18:13
 * Description: sys-контроллер для доступа к юзерам.
 */
object SysPerson extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Генерация экземпляра EmailActivation с бессмысленными данными. */
  private def dummyEa = EmailActivation(email = "admin@suggest.io", key = "keyKeyKeyKeyKey", id = Some("IdIdIdIdId888"))

  def index = IsSuperuser { implicit request =>
    Ok(indexTpl())
  }

  /** Отрендерить на экран email-сообщение регистрации юзера. */
  def showRegEmail = IsSuperuser { implicit request =>
    Ok(emailRegMsgTpl(dummyEa))
  }

  /** Отрендерить email-сообщение восстановления пароля. */
  def showRecoverEmail = IsSuperuser { implicit request =>
    Ok(emailPwRecoverTpl(dummyEa))
  }

}
