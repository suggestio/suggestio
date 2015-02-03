package controllers.ident

import controllers.SioController
import models.usr.{MPersonIdent, EmailPwIdent}
import play.api.data._
import play.api.data.Forms._
import util.acl._
import util._
import play.api.mvc._
import util.ident.IdentUtil
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
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
    val personId = request.pwOpt.get.personId
    lazy val logPrefix = s"_changePasswordSubmit($personId): "
    changePasswordFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(logPrefix + "Failed to bind form:\n " + formatFormErrors(formWithErrors))
        onError(formWithErrors)
      },
      {case (oldPw, newPw) =>
        // Нужно проверить старый пароль, если юзер есть в базе.
        val savedIds: Future[Seq[String]] = EmailPwIdent.findByPersonId(personId).flatMap { epws =>
          if (epws.isEmpty) {
            // Юзер меняет пароль, но залогинен через внешние сервисы. Нужно вычислить email и создать EmailPwIdent.
            MPersonIdent.findAllEmails(personId) flatMap { emails =>
              if (emails.isEmpty) {
                LOGGER.warn("Unknown user session: " + personId)
                Future successful Seq.empty[String]
              } else {
                Future.traverse(emails) { email =>
                  val epw = EmailPwIdent(email = email, personId = personId, pwHash = MPersonIdent.mkHash(newPw), isVerified = true)
                  val fut = epw.save
                  fut onSuccess {
                    case epwId =>
                      LOGGER.info(s"${logPrefix}Created new epw-ident $epwId for non-pw email $email")
                  }
                  fut
                }
              }
            }

          } else {
            // Юзер меняет пароль, но у него уже есть EmailPw-логины на s.io.
            val result = epws
              .find { _.checkPassword(oldPw) }
              .map { _.copy(pwHash = MPersonIdent.mkHash(newPw)) }
            result match {
              case Some(epw) =>
                epw.save.map { Seq(_) }
              case None =>
                LOGGER.warn(logPrefix + "No idents with email found for user " + personId)
                Future successful Seq.empty[String]
            }
          }
        }

        savedIds flatMap {
          case nil if nil.isEmpty =>
            val formWithErrors = changePasswordFormM.withGlobalError("error.password.invalid")
            onError(formWithErrors)

          case ids =>
            RdrBackOrFut(r)(changePwOkRdrDflt)
              .map { _.flashing("success" -> "Новый пароль сохранён.") }
        }
      }
    )
  }

}


