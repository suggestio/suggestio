package controllers.ident

import controllers.SioController
import models._
import models.usr.{EmailPwConfirmInfo, MPersonIdent, IEaEmailId, EmailActivation}
import play.api.data.Form
import play.api.data.Forms._
import controllers.Captcha._
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Result
import util.{FormUtil, PlayMacroLogsI}
import util.acl._
import util.mail.MailerWrapper
import views.html.ident._
import views.html.ident.reg.{_epwConfirmColTpl, _regSuccessColumnTpl}
import views.html.ident.reg.email._
import util.SiowebEsUtil.client

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 18:04
 * Description: Поддержка регистрации по имени и паролю в контроллере.
 */
object EmailPwReg {

  /** Маппинг формы регистрации по email. Форма с капчей. */
  def emailRegFormM: EmailPwRegReqForm_t = Form(
    mapping(
      "email"           -> email,
      CAPTCHA_ID_FN     -> captchaIdM,
      CAPTCHA_TYPED_FN  -> captchaTypedM
    )
    {(email1, _, _) => email1 }
    {email1 => Some((email1, "", ""))}
  )

  /** Форма подтверждения регистрации по email и паролю. */
  def epwRegConfirmFormM: EmailPwConfirmForm_t = Form(
    mapping(
      "nodeName" -> FormUtil.nameM,
      "password" -> FormUtil.passwordWithConfirmM
    )
    { EmailPwConfirmInfo.apply }
    { EmailPwConfirmInfo.unapply }
  )

}


import EmailPwReg._


trait EmailPwReg extends SioController with PlayMacroLogsI {

  def sendEmailAct(ea: EmailActivation)(implicit ctx: Context): Unit = {
    val msg = MailerWrapper.instance
    msg.setFrom("welcome@suggest.io")
    msg.setRecipients(ea.email)
    msg.setSubject("Suggest.io | " + Messages("reg.emailpw.email.subj")(ctx.lang))  // TODO Заголовок в messages и сюда!
    msg.setHtml( emailRegMsgTpl(ea)(ctx) )
    msg.send()
  }

  /**
   * Сабмит формы регистрации по email.
   * Нужно отправить письмо на указанный ящик и отредиректить юзера на страницу с инфой.
   * @return emailRegFormBindFailed() при проблеме с маппингом формы.
   *         emailRequestOk() когда сообщение отправлено почтой.
   */
  def emailRegSubmit = IsAnonPost.async { implicit request =>
    emailRegFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug("emailRegSubmit(): Failed to bind form:\n " + formatFormErrors(formWithErrors))
        val ctx = implicitly[Context]
        val rc = _regColumnTpl(formWithErrors)(ctx)
        NotAcceptable( mySioStartTpl(Seq(rc))(ctx) )
      },
      {email1 =>
        // Почта уже зарегана может?
        MPersonIdent.findIdentsByEmail(email1) flatMap {
          // Нет такого email. Собираем активацию.
          case nil if nil.isEmpty =>    // Используем isEmpty во избежания скрытых изменений в API в будущем
            // Сохранить новый eact
            val ea0 = EmailActivation(
              email = email1,
              key = CanConfirmEmailPwReg.EPW_ACT_KEY
            )
            ea0.save.flatMap { eaId =>
              // отправить письмо на указанную почту
              val ea1 = ea0.copy(id = Some(eaId))
              sendEmailAct(ea1)
              // Вернуть ответ юзеру
              emailRequestOk(Some(ea1))
            }

          // Уже есть такой email в базе. Активация не требуется, вроде.
          case idents =>
            LOGGER.error(s"emailRegSubmit($email1): Email already exists.")
            emailRequestOk(None)
        }
      }
    )
  }

  /** Что возвращать юзеру, когда сообщение отправлено на почту? */
  protected def emailRequestOk(ea: Option[EmailActivation])(implicit ctx: Context): Future[Result] = {
    Ok(sentTpl(ea)(ctx))
  }


  /** Юзер возвращается по ссылке из письма. Отрендерить страницу завершения регистрации. */
  def emailReturn(eaInfo: IEaEmailId) = CanConfirmEmailPwRegGet(eaInfo) { implicit request =>
    // ActionBuilder уже выверил всё. Нужно показать юзеру страницу с формой ввода пароля, названия узла и т.д.
    val ctx = implicitly[Context]
    val rc = _epwConfirmColTpl(request.ea, epwRegConfirmFormM)(ctx)
    val page = mySioStartTpl( Seq(rc) )(ctx)
    Ok(page)
  }

  /** Сабмит формы подтверждения регистрации по email. */
  def emailConfirmSubmit(eaInfo: IEaEmailId) = CanConfirmEmailPwRegPost(eaInfo) { implicit request =>
    // ActionBuilder выверил данные из письма, надо забиндить данные регистрации, создать узел и т.д.
    // TODO Создать юзера, удалить активацию, создать новый узел-ресивер.
    val ctx = implicitly[Context]
    val col = _regSuccessColumnTpl()(ctx)
    Ok( mySioStartTpl(Seq(col))(ctx) )
  }

}
