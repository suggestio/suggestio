package controllers.ident

import controllers.SioController
import models.msession.{Ttl, ShortTtl, LongTtl, Keys}
import models.usr._
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
    Form(
      mapping(
        "email"       -> email,
        "password"    -> passwordM,
        "remember_me" -> boolean.transform[Ttl](
          { rme => if (rme) LongTtl else ShortTtl },
          { _ == LongTtl }
        )
      )
      { EpwLoginFormBind.apply }
      { EpwLoginFormBind.unapply }
    )
  }

  /** Маппинг формы epw-логина с забиванием дефолтовых значений. */
  def emailPwLoginFormStubM(implicit request: RequestHeader): Future[EmailPwLoginForm_t] = {
    // Пытаемся извлечь email из сессии.
    val emailFut: Future[String] = request.session.get(Keys.PersonId.name) match {
      case Some(personId) =>
        EmailPwIdent.findByPersonId(personId)
          .map { _.headOption.fold("")(_.email) }
      case None =>
        Future successful ""
    }
    emailFut map { email1 =>
      emailPwLoginFormM fill EpwLoginFormBind(email1, "", LongTtl)
    }
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
      val formBinded = emailPwLoginFormM.bindFromRequest()
      formBinded.fold(
        {formWithErrors =>
          LOGGER.debug("emailPwLoginFormSubmit(): Form bind failed:\n" + formatFormErrors(formWithErrors))
          emailSubmitError(formWithErrors, r)
        },
        {binded =>
          EmailPwIdent.getByEmail(binded.email) flatMap { epwOpt =>
            if (epwOpt.exists(_.checkPassword(binded.password))) {
              // Логин удался.
              // TODO Нужно дать возможность режима сессии "чужой компьютер".
              val personId = epwOpt.get.personId
              val rdrFut = RdrBackOrFut(r) { emailSubmitOkCall(personId) }
              var addToSession: List[(String, String)] = List(
                Keys.PersonId.name -> personId
              )
              // Реализация длинной сессии при наличии флага rememberMe.
              addToSession = binded.ttl.addToSessionAcc(addToSession)
              rdrFut
                .map { _.addingToSession(addToSession : _*) }
            } else {
              val binded1 = binded.copy(password = "")
              val lf = formBinded
                .fill(binded1)
                .withGlobalError("error.unknown.email_pw")
              emailSubmitError(lf, r)
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
  def emailPwLoginForm(r: Option[String]) = IsAnonGet.async { implicit request =>
    emailPwLoginFormStubM map { lf =>
      epwLoginPage(lf, r)
    }
  }

  /** Общий код методов emailPwLoginForm() и emailSubmitError(). */
  protected def epwLoginPage(lf: EmailPwLoginForm_t, r: Option[String])
                            (implicit request: AbstractRequestWithPwOpt[_]): Result = {
    val ctx = implicitly[Context]
    val column = _loginColumnTpl(lf, r)(ctx)
    Ok( mySioStartTpl(Seq(column))(ctx) )
  }

  override def emailSubmitError(lf: EmailPwLoginForm_t, r: Option[String])
                               (implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    epwLoginPage(lf, r)
  }

}
