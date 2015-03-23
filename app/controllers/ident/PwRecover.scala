package controllers.ident

import controllers.{routes, CaptchaValidator, SioController}
import models.msession.Keys
import models.usr.{MPersonIdent, EmailActivation, EmailPwIdent}
import play.api.data._
import play.twirl.api.Html
import util.PlayMacroLogsI
import util.acl._
import util.ident.IdentUtil
import util.mail.MailerWrapper
import views.html.helper.CSRF
import views.html.ident.mySioStartTpl
import views.html.ident.recover._
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Future
import models._
import play.api.i18n.Messages
import util.SiowebEsUtil.client
import util.FormUtil.passwordWithConfirmM

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 16:23
 * Description: Аддон для Ident-контроллера для поддержки восстановления пароля.
 */

object PwRecover {

  /** Маппинг формы восстановления пароля. */
  def recoverPwFormM: EmailPwRecoverForm_t = {
    EmailPwReg.emailRegFormM
  }

}


import PwRecover._


/** Хелпер контроллеров, занимающийся отправкой почты для восстановления пароля. */
trait SendPwRecoverEmail extends SioController {

  /**
   * Отправка письма юзеру. Это статический метод, но он сильно завязан на внутренности sio-контроллеров,
   * поэтому он реализован как аддон для контроллеров.
   * @param email1 email юзера.
   * @return Фьючерс для синхронизации.
   */
  protected def sendRecoverMail(email1: String)(implicit request: AbstractRequestWithPwOpt[_]): Future[_] = {
    // Надо найти юзера в базах PersonIdent, и если есть, то отправить письмецо.
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
            msg.setHtml(emailPwRecoverTpl(eact2)(ctx))
            msg.send()
          }
        }
      } else {
        // TODO Если юзера нет, то создать его и тоже отправить письмецо с активацией? или что-то иное вывести?
        Future successful None // None вместо Unit(), чтобы 2.11 компилятор не ругался.
      }
    }
  }

}


trait PwRecover extends SendPwRecoverEmail with PlayMacroLogsI with CaptchaValidator with BruteForceProtectCtl {

  protected def _outer(html: Html)(implicit ctx: Context): Html = {
    mySioStartTpl(
      title     = Messages("Password.recovery")(ctx.lang),
      columns   = Seq(html)
    )(ctx)
  }

  protected def _recoverPwStep1(form: EmailPwRecoverForm_t)(implicit request: AbstractRequestWithPwOpt[_]): Html = {
    val ctx = implicitly[Context]
    val colHtml = _emailColTpl(form)(ctx)
    _outer(colHtml)(ctx)
  }

  /** Запрос страницы с формой вспоминания пароля по email'у. */
  def recoverPwForm = IsAnonGet { implicit request =>
    Ok(_recoverPwStep1(recoverPwFormM))
  }

  /** Сабмит формы восстановления пароля. */
  def recoverPwFormSubmit = IsAnonPost.async { implicit request =>
    bruteForceProtected {
      val formBinded = checkCaptcha(recoverPwFormM.bindFromRequest())
      formBinded.fold(
        {formWithErrors =>
          LOGGER.debug("recoverPwFormSubmit(): Failed to bind form:\n" + formatFormErrors(formWithErrors))
          NotAcceptable(_recoverPwStep1(formWithErrors))
        },
        {email1 =>
          sendRecoverMail(email1) map { _ =>
            // отрендерить юзеру результат, что всё ок, независимо от успеха поиска.
            rmCaptcha(formBinded){
              Redirect( CSRF(routes.Ident.recoverPwAccepted(email1)) )
            }
          }
        }
      )
    }
  }

  /** Рендер страницы, отображаемой когда запрос восстановления пароля принят.
    * CSRF используется, чтобы никому нельзя было слать ссылку с сообщением "ваш пароль выслан вам на почту". */
  def recoverPwAccepted(email1: String) = MaybeAuthPost { implicit request =>
    val ctx = implicitly[Context]
    val colHtml = _acceptedColTpl(email1)(ctx)
    val html = _outer(colHtml)(ctx)
    Ok(html)
  }

  /** Форма сброса пароля. */
  private def pwResetFormM: PwResetForm_t = Form(passwordWithConfirmM)

  protected def _pwReset(form: PwResetForm_t)(implicit request: RecoverPwRequest[_]): Html = {
    val ctx = implicitly[Context]
    val colHtml = _pwResetColTpl(form, request.eAct)(ctx)
    _outer(colHtml)(ctx)
  }

  /** Юзер перешел по ссылке восстановления пароля из письма. Ему нужна форма ввода нового пароля. */
  def recoverPwReturn(eActId: String) = CanRecoverPwGet(eActId) { implicit request =>
    Ok(_pwReset(pwResetFormM))
  }

  /** Юзер сабмиттит форму с новым паролем. Нужно его залогинить, сохранить новый пароль в базу,
    * удалить запись из EmailActivation и отредиректить куда-нибудь. */
  def pwResetSubmit(eActId: String) = CanRecoverPwPost(eActId).async { implicit request =>
    pwResetFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"pwResetSubmit($eActId): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        NotAcceptable(_pwReset(formWithErrors))
      },
      {newPw =>
        val pwHash2 = MPersonIdent.mkHash(newPw)
        val epw2 = request.epw.copy(pwHash = pwHash2, isVerified = true)
        // Запускаем сохранение новых данных по паролю
        val updateFut = epw2.save flatMap { _ =>
          request.eAct.delete
        }
        // Генерить ответ как только появляется возможность.
        val resFut = IdentUtil.redirectUserSomewhere(epw2.personId) map { rdr =>
          rdr.addingToSession(Keys.PersonId.name -> epw2.personId)
            .flashing("success" -> "New.password.saved")
        }
        val res2Fut = IdentBase.setLangCookie1(resFut, epw2.personId)
        // Дожидаемся завершения всех асинхронных операций и возвращаем результат.
        updateFut flatMap { _ =>
          res2Fut
        }
      }
    )
  }

}


