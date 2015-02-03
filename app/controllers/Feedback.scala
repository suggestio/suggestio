package controllers

import util.mail.MailerWrapper
import util.PlayLazyMacroLogsImpl
import play.api.data._
import play.api.data.Forms._
import util.HtmlSanitizer.supportMsgPolicy
import util.FormUtil._
import views.html.feedback._
import play.api.Play.{current, configuration}
import play.api.i18n.Messages
import util.acl._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.06.13 16:55
 * Description: Контроллер обратной связи с посетителями сайта.
 * Пока что тут только отправка писем с сайта.
 */

object Feedback extends SioController with PlayLazyMacroLogsImpl with CaptchaValidator {

  import LOGGER._

  val FEEDBACK_RCVR_EMAILS = configuration.getStringSeq("feedback.send.to.emails") getOrElse Seq("support@suggest.io")

  val REPLY_TO_HDR = "Reply-To"

  /** Форма написания сообщения. */
  val feedbackSubmitFormM = Form(tuple(
    "email"   -> email,
    "message" -> nonEmptyText(minLength = 10, maxLength = 10000)
      .transform(supportMsgPolicy.sanitize(_).trim, strIdentityF),
    CAPTCHA_ID_FN     -> Captcha.captchaIdM,
    CAPTCHA_TYPED_FN  -> Captcha.captchaTypedM
  ))


  /**
   * Отрендерить страницу/форму обратной связи.
   * @param isAsync если true, то будет отрендерено inline. Если false, то на выходе будет страница.
   * @return Форма в виде страницы ИЛИ в виде inline-формы в зависимости от isAsync.
   */
  def feedbackForm(isAsync:Boolean) = MaybeAuth { implicit request =>
    val render = if (isAsync) {
      _feedbackFormTpl(feedbackSubmitFormM)
    } else {
      feedbackTpl(feedbackSubmitFormM)
    }
    Ok(render)
  }


  /**
   * Сабмит формы обратной связи. Отправить по email письмо на support@suggest.io.
   * @return Редирект куда-нибудь + flash.
   */
  def feedbackFormSubmit = MaybeAuth { implicit request =>
    import request.pwOpt
    val formBinded = checkCaptcha( feedbackSubmitFormM.bindFromRequest() )
    formBinded.fold(
      {formWithErrors =>
        debug("feedbackFormSubmit(): Failed to bind form: " + formatFormErrors(formWithErrors))
        NotAcceptable(feedbackTpl(formWithErrors))
      },
      {case (email1, message, captchaId, captchaTyped) =>
        // Отправить письмо на ящик suggest.io.
        val msg = MailerWrapper.instance
        // Разделять сабжи в зависимости от залогиненности юзеров.
        val subjectEnd = pwOpt match {
          case Some(pw) => "клиента " + pw.personId
          case None     => "посетителя сайта"
        }
        msg.setSubject("Сообщение от " + subjectEnd)
        msg.setFrom(email1)
        msg.setReplyTo(email1)
        msg.setRecipients(FEEDBACK_RCVR_EMAILS : _*)
        val ctx = getContext2
        msg.setText( feedbackMailTxtTpl(email1, message)(ctx) )
        msg.send()
        rmCaptcha(formBinded) {
          // Отредиректить юзера куда-нибудь на главную, стерев капчу из кукисов.
          Redirect(models.MAIN_PAGE_CALL)
            .flashing("success" -> Messages("f.feedback_sent_success")(ctx.lang))
        }
      }
    )
  }

}
