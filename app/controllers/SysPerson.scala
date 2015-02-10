package controllers

import models.usr.EmailActivation
import util.PlayMacroLogsImpl
import util.acl.IsSuperuser
import views.html.ident.reg.email.emailRegMsgTpl
import views.html.ident.recover.emailPwRecoverTpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.02.15 18:13
 * Description: sys-контроллер для доступа к юзерам.
 */
object SysPerson extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  private def dummyEa = EmailActivation(email = "admin@suggest.io", key = "keyKeyKeyKeyKey", id = Some("IdIdIdIdId888"))

  def showRegEmail = IsSuperuser { implicit request =>
    Ok(emailRegMsgTpl(dummyEa))
  }

  def showRecoverEmail = IsSuperuser { implicit request =>
    Ok(emailPwRecoverTpl(dummyEa))
  }

}
