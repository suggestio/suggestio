package controllers.ident

import controllers.{routes, CaptchaValidator, SioController}
import play.api.data._
import util.acl._
import util._
import util.ident.IdentUtil
import util.mail.MailerWrapper
import views.html.ident.recover._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import models._
import play.api.mvc.Security.username
import play.api.i18n.Messages
import SiowebEsUtil.client
import FormUtil.passwordWithConfirmM

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 16:23
 * Description: Аддон для Ident-контроллера для поддержки восстановления пароля.
 */

object PwRecover {

  def recoverPwFormM = EmailPwReg.emailRegFormM

}


import PwRecover._


trait PwRecover extends SioController with PlayMacroLogsI with CaptchaValidator with BruteForceProtectCtl {

  /** Запрос страницы с формой вспоминания пароля по email'у. */
  def recoverPwForm = IsAnon { implicit request =>
    Ok(recoverPwFormTpl(recoverPwFormM))
  }

  /** Сабмит формы восстановления пароля. */
  def recoverPwFormSubmit = IsAnon.async { implicit request =>
    bruteForceProtected {
      val formBinded = checkCaptcha(recoverPwFormM.bindFromRequest())
      formBinded.fold(
        {formWithErrors =>
          LOGGER.debug("recoverPwFormSubmit(): Failed to bind form:\n" + formatFormErrors(formWithErrors))
          NotAcceptable(recoverPwFormTpl(formWithErrors))
        },
        {email1 =>
          // TODO Надо найти юзера в базах EmailPwIdent и MozPersonaIdent, и если есть, то отправить письмецо.
          MPersonIdent.findIdentsByEmail(email1) flatMap { idents =>
            if (idents.nonEmpty) {
              val emailIdentFut: Future[EmailPwIdent] = idents
                .foldLeft[List[EmailPwIdent]](Nil) {
                case (acc, epw: EmailPwIdent) => epw :: acc
                case (acc, _) => acc
              }
                .headOption
                .map { Future.successful }
                .getOrElse {
                  // берём personId из moz persona. Там в списке только один элемент, т.к. email является уникальным в рамках ident-модели.
                  val personId = idents.map(_.personId).head
                  val epw = EmailPwIdent(email = email1, personId = personId, pwHash = "", isVerified = false)
                  epw.save
                    .map { _ => epw}
                }
              emailIdentFut flatMap { epwIdent =>
                // Нужно сгенерить ключ для восстановления пароля. И ссылку к нему.
                val eact = EmailActivation(email = email1, key = epwIdent.personId)
                eact.save.map { eaId =>
                  val eact2 = eact.copy(
                    id = Some(eaId)
                  )
                  // Можно отправлять письмецо на ящик.
                  val msg = MailerWrapper.instance
                  msg.setFrom("no-reply@suggest.io")
                  msg.setRecipients(email1)
                  val ctx = implicitly[Context]
                  msg.setSubject("Suggest.io | " + Messages("Password.recovery")(ctx.lang))
                  msg.setText( views.txt.ident.recover.emailPwRecoverTpl(eact2)(ctx) )
                  msg.setHtml( emailPwRecoverTpl(eact2)(ctx) )
                  msg.send()
                }
              }
            } else {
              // TODO Если юзера нет, то создать его и тоже отправить письмецо с активацией? или что-то иное вывести?
              Future successful None  // None вместо Unit(), чтобы 2.11 компилятор не ругался.
            }
          } map { _ =>
            // отрендерить юзеру результат, что всё ок, независимо от успеха поиска.
            rmCaptcha(formBinded){
              Redirect(routes.Ident.recoverPwAccepted(email1))
            }
          }
        }
      )
    }
  }

  /** Рендер страницы, отображаемой когда запрос восстановления пароля принят. */
  def recoverPwAccepted(email1: String) = MaybeAuth { implicit request =>
    Ok(pwRecoverAcceptedTpl(email1))
  }

  /** Форма сброса пароля. */
  private def pwResetFormM = Form(passwordWithConfirmM)

  /** Юзер перешел по ссылке восстановления пароля из письма. Ему нужна форма ввода нового пароля. */
  def recoverPwReturn(eActId: String) = CanRecoverPw(eActId).async { implicit request =>
    MarketIndexAccess.getNodes map { nodes =>
      Ok(pwResetTpl(request.eAct, pwResetFormM, nodes))
    }
  }

  /** Юзер сабмиттит форму с новым паролем. Нужно его залогинить, сохранить новый пароль в базу,
    * удалить запись из EmailActivation и отредиректить куда-нибудь. */
  def pwResetSubmit(eActId: String) = CanRecoverPw(eActId).async { implicit request =>
    pwResetFormM.bindFromRequest().fold(
      {formWithErrors =>
        val nodesFut = MarketIndexAccess.getNodes
        LOGGER.debug(s"pwResetSubmit($eActId): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        nodesFut map { nodes =>
          NotAcceptable(pwResetTpl(request.eAct, formWithErrors, nodes))
        }
      },
      {newPw =>
        val pwHash2 = MPersonIdent.mkHash(newPw)
        val epw2 = request.epw.copy(pwHash = pwHash2, isVerified = true)
        for {
          _   <- epw2.save
          _   <- request.eAct.delete
          rdr <- IdentUtil.redirectUserSomewhere(epw2.personId)
        } yield {
          rdr.withSession(username -> epw2.personId)
            .flashing("success" -> "Новый пароль сохранён.")
        }
      }
    )
  }

}


