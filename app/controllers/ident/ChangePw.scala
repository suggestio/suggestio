package controllers.ident

import controllers.SioController
import play.api.data._
import play.api.data.Forms._
import util.acl._
import util._
import play.api.mvc._
import util.ident.IdentUtil
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import models._
import SiowebEsUtil.client
import FormUtil.{passwordM, passwordWithConfirmM}
import views.html.ident.changePasswordTpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 16:30
 * Description: Поддержка смены пароля для Ident контроллера и других контроллеров.
 */

object ChangePw {

  /** Маппинг формы смены пароля. */
  def changePasswordFormM = Form(tuple(
    "old" -> passwordM,
    "new" -> passwordWithConfirmM
  ))

}


import ChangePw._


/** Ident-контроллер придерживается этих экшенов. */
trait ChangePw extends ChangePwAction {

  /** Страница с формой смены пароля. */
  def changePassword = IsAuth { implicit request =>
    Ok(changePasswordTpl(changePasswordFormM))
  }

  def changePasswordSubmit(r: Option[String]) = IsAuth.async { implicit request =>
    _changePasswordSubmit(r) { formWithErrors =>
      NotAcceptable(changePasswordTpl(formWithErrors))
    }
  }

}


/** Контексто-зависимое тело экшена, которое реализует смену пароля у пользователя.
  * Реализации должны оборачивать логику экшена в экшен, выставляя обработчики для ошибок и успехов. */
trait ChangePwAction extends SioController with PlayMacroLogsI {

  /** Если неясно куда надо редиректить юзера, то что делать? */
  def changePwOkRdrDflt(implicit request: AbstractRequestWithPwOpt[AnyContent]): Future[Call] = {
    // TODO Избавится от get, редиректя куда-нить в другое место.
    IdentUtil.redirectCallUserSomewhere(request.pwOpt.get.personId)
  }

  /** Сабмит формы смены пароля. Нужно проверить старый пароль и затем заменить его новым. */
  def _changePasswordSubmit(r: Option[String])(onError: Form[(String, String)] => Future[Result])
                           (implicit request: AbstractRequestWithPwOpt[AnyContent]): Future[Result] = {
    changePasswordFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug("changePasswordSubmit(): Failed to bind form:\n" + formatFormErrors(formWithErrors))
        onError(formWithErrors)
      },
      {case (oldPw, newPw) =>
        // Нужно проверить старый пароль, если юзер есть в базе.
        val personId = request.pwOpt.get.personId
        EmailPwIdent.findByPersonId(personId).flatMap { epws =>
          if (epws.isEmpty) {
            // Юзер меняет пароль, но залогинен через moz persona.
            MozillaPersonaIdent.findByPersonId(personId)
              .map { mps =>
                if (mps.isEmpty) {
                  LOGGER.warn("changePasswordSubmit(): Unknown user session: " + personId)
                  None
                } else {
                  val mp = mps.head
                  val epw = EmailPwIdent(email = mp.email, personId = mp.personId, pwHash = MPersonIdent.mkHash(newPw), isVerified = true)
                  Some(epw)
                }
              }
          } else {
            // Юзер меняет пароль, но у него уже есть EmailPw-логины на s.io.
            val result = epws
              .find { _.checkPassword(oldPw) }
              .map { _.copy(pwHash = MPersonIdent.mkHash(newPw)) }
            Future successful result
          }
        } flatMap {
          case Some(epw) =>
            epw.save
              .flatMap { _ => RdrBackOrFut(r)(changePwOkRdrDflt) }
              .map { _.flashing("success" -> "Новый пароль сохранён.") }
          case None =>
            val formWithErrors = changePasswordFormM.withGlobalError("error.password.invalid")
            onError(formWithErrors)
        }
      }
    )
  }

}


