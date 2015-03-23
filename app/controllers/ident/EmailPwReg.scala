package controllers.ident

import controllers.{CaptchaValidator, SioController}
import models._
import models.msession.Keys
import models.usr._
import play.api.data.Form
import play.api.data.Forms._
import controllers.Captcha._
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Result
import util.adn.NodesUtil
import util.{FormUtil, PlayMacroLogsI}
import util.acl._
import util.mail.MailerWrapper
import views.html.ident.reg.regSuccessTpl
import views.html.ident.reg.email._
import util.SiowebEsUtil.client
import play.api.Play.current

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
      "pw"       -> FormUtil.passwordWithConfirmM
    )
    { EmailPwConfirmInfo.apply }
    { EmailPwConfirmInfo.unapply }
  )

}


import EmailPwReg._


trait EmailPwReg extends SioController with PlayMacroLogsI with CaptchaValidator with SendPwRecoverEmail {

  def sendEmailAct(ea: EmailActivation)(implicit ctx: Context): Unit = {
    val msg = MailerWrapper.instance
    msg.setFrom("no-reply@suggest.io")
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
    val form1 = checkCaptcha( emailRegFormM.bindFromRequest() )
    form1.fold(
      {formWithErrors =>
        LOGGER.debug("emailRegSubmit(): Failed to bind form:\n " + formatFormErrors(formWithErrors))
        NotAcceptable( epwRegTpl(formWithErrors, captchaShown = true) )
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

          // Уже есть такой email в базе. Выслать восстановление пароля.
          case idents =>
            LOGGER.error(s"emailRegSubmit($email1): Email already exists.")
            sendRecoverMail(email1) flatMap { _ =>
              emailRequestOk(None)
            }
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
    Ok(confirmTpl(request.ea, epwRegConfirmFormM))
  }

  /** Сабмит формы подтверждения регистрации по email. */
  def emailConfirmSubmit(eaInfo: IEaEmailId) = CanConfirmEmailPwRegPost(eaInfo).async { implicit request =>
    // ActionBuilder выверил данные из письма, надо забиндить данные регистрации, создать узел и т.д.
    epwRegConfirmFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"emailConfirmSubmit($eaInfo): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        NotAcceptable(confirmTpl(request.ea, formWithErrors))
      },
      {data =>
        // Создать юзера и его ident, удалить активацию, создать новый узел-ресивер.
        val lang = request2lang
        MPerson(lang = lang.code).save flatMap { personId =>
          // Развернуть узел для юзера
          val adnNodeFut = NodesUtil.createUserNode(name = data.adnName, personId = personId)
          // Сохранить новый epw-ident
          val idSaveFut = EmailPwIdent(
            email       = eaInfo.email,
            personId    = personId,
            pwHash      = MPersonIdent.mkHash(data.password),
            isVerified  = true
          ).save
          // Рендерим результат запроса сразу как только нода будет готова к использованию.
          val resFut = adnNodeFut map { adnNode =>
            Ok(regSuccessTpl(adnNode))
              .addingToSession(Keys.PersonId.name -> personId)
              .withLang(lang)
          }
          // Дожидаемся завершения всех операций и возвращаем результат.
          request.ea.delete flatMap { _ =>
            idSaveFut flatMap { _ =>
              resFut
            }
          }
        }  // Mperson.save
      }
    )   // Form.fold
  }

}
