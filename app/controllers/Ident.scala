package controllers

import controllers.ident._
import play.api.data.Form
import util.acl._
import util._
import play.api.mvc._
import util.ident.IdentUtil
import views.html.ident._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import models._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 11:47
 * Description: Контроллер обычного логина в систему.
 * Обычно логинятся через email+password.
 * 2015.jan.27: вынос разжиревших кусков контроллера в util.acl.*, controllers.ident.* и рефакторинг.
 */

object Ident extends SioController with PlayMacroLogsImpl with EmailPwSubmit with CaptchaValidator
with ChangePw with PwRecover with EmailPwReg {

  import LOGGER._

  /**
   * Юзер разлогинивается. Выпилить из сессии данные о его логине.
   * @return Редирект на главную, ибо анонимусу идти больше некуда.
   */
  def logout = Action { implicit request =>
    Redirect(MAIN_PAGE_CALL)
      .withNewSession
  }


  /** Рендер страницы с возможностью логина по email и паролю. */
  def emailPwLoginForm(r: Option[String]) = IsAnon { implicit request =>
    Ok(emailPwLoginFormTpl(EmailPwSubmit.emailPwLoginFormM, r))
  }


  override def emailSubmitError(lf: EmailPwLoginForm_t, r: Option[String])
                               (implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    Forbidden(emailPwLoginFormTpl(lf, r))
  }


  /** Отредиректить юзера куда-нибудь. */
  def rdrUserSomewhere = IsAuth.async { implicit request =>
    IdentUtil.redirectUserSomewhere(request.pwOpt.get.personId)
  }


  /** Что рендерить при неудачном биндинге формы регистрации? */
  override def emailRegFormBindFailed(formWithErrors: Form[String])
                                     (implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    ???
  }
}

