package controllers.ident

import controllers.SioController
import models.usr.EmailPwIdent
import play.api.data._
import play.api.data.Forms._
import util.acl._
import util._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import util.ident.IdentUtil
import views.html.ident.{mySioStartTpl, _loginColumnTpl}
import scala.concurrent.Future
import models._
import play.api.mvc.Security.username
import SiowebEsUtil.client
import util.FormUtil.passwordM

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 16:18
 * Description: Поддержка сабмита формы логина по email и паролю.
 */

object EmailPwSubmit {

  /** Форма логина по email и паролю. */
  def emailPwLoginFormM: EmailPwLoginForm_t = {
    Form(tuple(
      "email"    -> email,
      "password" -> passwordM
    ))
  }

}


import EmailPwSubmit._


/** Добавить обработчик сабмита формы логина по email и паролю в контроллер. */
trait EmailPwSubmit extends SioController with PlayMacroLogsI with BruteForceProtectCtl {

  def emailSubmitOkCall(personId: String)(implicit request: AbstractRequestWithPwOpt[_]): Future[Call] = {
    IdentUtil.redirectCallUserSomewhere(personId)
  }

  def emailSubmitError(lf: EmailPwLoginForm_t, r: Option[String])(implicit request: AbstractRequestWithPwOpt[_]): Future[Result]

  /** Самбит формы логина по email и паролю. */
  def emailPwLoginFormSubmit(r: Option[String]) = IsAnonPost.async { implicit request =>
    bruteForceProtected {
      emailPwLoginFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug("emailPwLoginFormSubmit(): Form bind failed:\n" + formatFormErrors(formWithErrors))
          emailSubmitError(formWithErrors, r)
        },
        {case (email1, pw1) =>
          EmailPwIdent.getByEmail(email1) flatMap { epwOpt =>
            if (epwOpt.exists(_.checkPassword(pw1))) {
              // Логин удался.
              // TODO Нужно дать возможность режима сессии "чужой компьютер".
              val personId = epwOpt.get.personId
              RdrBackOrFut(r) { emailSubmitOkCall(personId) }
                .map { _.withSession(username -> personId) }
            } else {
              val lf = emailPwLoginFormM.fill(email1 -> "")
              val lfe = lf.withGlobalError("error.unknown.email_pw")
              emailSubmitError(lfe, r)
            }
          }
        }
      )
    }
  }

}


/** Экшены для Ident-контроллера. */
trait EmailPwLogin extends EmailPwSubmit {

  /** Рендер страницы с возможностью логина по email и паролю. */
  def emailPwLoginForm(r: Option[String]) = IsAnonGet { implicit request =>
    val lf = emailPwLoginFormM
    epwLoginPage(lf, r)
  }

  /** Общий код методов emailPwLoginForm() и emailSubmitError(). */
  protected def epwLoginPage(lf: EmailPwLoginForm_t, r: Option[String])
                            (implicit request: AbstractRequestWithPwOpt[_]): Result = {
    val ctx = implicitly[Context]
    val column = _loginColumnTpl(lf)(ctx)
    Ok( mySioStartTpl(Seq(column))(ctx) )
  }

  override def emailSubmitError(lf: EmailPwLoginForm_t, r: Option[String])
                               (implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    epwLoginPage(lf, r)
  }

}
