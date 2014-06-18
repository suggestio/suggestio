package controllers

import play.api.mvc.Controller
import util.{PlayLazyMacroLogsImpl, ContextT}
import com.typesafe.plugin.{use, MailerPlugin}
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

object Feedback extends SioController with PlayLazyMacroLogsImpl with ContextT with CaptchaValidator {

  import LOGGER._

  val CAPTCHA_ID_FN = "captchaId"
  val CAPTCHA_TYPED_FN = "captchaTyped"

  /**
   * Форма написания сообщения.
   */
  val feedbackSubmitFormM = Form(tuple(
    "email"   -> email,
    "message" -> nonEmptyText(minLength = 10, maxLength = 10000)
      .transform(supportMsgPolicy.sanitize(_).trim, strIdentityF),
    CAPTCHA_ID_FN -> nonEmptyText(maxLength = 16)
      .transform(strTrimSanitizeF, strIdentityF),
    CAPTCHA_TYPED_FN -> nonEmptyText(maxLength = 16)
      .transform(strTrimF, strIdentityF)
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
    val formBinded = checkCaptcha(feedbackSubmitFormM.bindFromRequest(), CAPTCHA_ID_FN, captchaValueFn = CAPTCHA_TYPED_FN)
    formBinded.fold(
      {formWithErrors =>
        debug("feedbackFormSubmit(): Failed to bind form: " + formatFormErrors(formWithErrors))
        NotAcceptable(feedbackTpl(formWithErrors))
      },
      {case (email1, message, captchaId, captchaTyped) =>
        // Отправить письмо на ящик suggest.io.
        val mail = use[MailerPlugin].email
        // Разделять сабжи в зависимости от залогиненности юзеров.
        val subjectEnd = pwOpt match {
          case Some(pw) => "клиента " + pw.personId
          case None     => "посетителя сайта"
        }
        mail.setSubject("Сообщение от " + subjectEnd)
        val rcvrs = configuration.getStringSeq("feedback.html.send.to.emails") getOrElse Seq("support@suggest.io")
        mail.setFrom(email1)
        mail.addHeader("Reply-To", email1)
        mail.setRecipient(rcvrs : _*)
        val ctx = getContext2
        mail.send(feedbackMailTxtTpl(email1, message)(ctx).toString())
        // Отредиректить юзера куда-нибудь, стерев капчу из кукисов.
        val resp = Redirect(routes.Application.index())
          .flashing("success" -> Messages("f.feedback_sent_success")(ctx.lang))
        rmCaptcha(formBinded, CAPTCHA_ID_FN, resp)
      }
    )
  }

}
