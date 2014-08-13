package controllers

import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client
import util.PlayLazyMacroLogsImpl
import util.acl.IsAuth
import views.html.market.lk.support._
import com.typesafe.plugin.{use, MailerPlugin}
import play.api.Play.{current, configuration}
import scala.concurrent.Future
import Feedback.{FEEDBACK_RCVR_EMAILS, REPLY_TO_HDR}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.14 14:39
 * Description: Контроллер для обратной связи с техподдержкой s.io в личном кабинете узла.
 */
object MarketLkSupport extends SioController with PlayLazyMacroLogsImpl {

  import LOGGER._


  /** Маппинг для формы обращения в саппорт. */
  private val supportFormM = {
    import play.api.data._, Forms._
    import util.FormUtil._
    Form(
      mapping(
        "name"  -> optional(nameM),
        "email" -> email,
        "msg"   -> text2048M,
        "phone" -> phoneOptM
      )
      { MLkSupportRequest.apply }
      { MLkSupportRequest.unapply }
    )
  }


  /** Отрендерить форму с запросом помощи.
    * @return 200 Ок и страница с формой.
    */
  def supportForm(adnIdOpt: Option[String], r: Option[String]) = IsAuth.async { implicit request =>
    // Взять дефолтовое значение email'а по сессии
    val emailsDfltFut = request.pwOpt.fold [Future[Seq[String]]]
      { Future successful Nil }
      { pw => MPersonIdent.findAllEmails(pw.personId) }
    emailsDfltFut map { emailsDflt =>
      val emailDflt = emailsDflt.headOption getOrElse ""
      val lsr = MLkSupportRequest(name = None, replyEmail = emailDflt, msg = "")
      val form = supportFormM fill lsr
      Ok(supportFormTpl(adnIdOpt, form, r))
    }
  }


  /**
   * Сабмит формы обращения за помощью.
   */
  def supportFormSubmit(adnIdOpt: Option[String], r: Option[String]) = IsAuth.async { implicit request =>
    lazy val logPrefix = s"supportFormSubmit(adnId=$adnIdOpt): "
    supportFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind lk-feedback form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(supportFormTpl(adnIdOpt, formWithErrors, r))
      },
      {lsr =>
        trace(logPrefix + "Processing from ip=" + request.remoteAddress)
        val userEmailsFut = MPersonIdent.findAllEmails(request.pwOpt.get.personId)
        val mail = use[MailerPlugin].email
        mail.addHeader(REPLY_TO_HDR, lsr.replyEmail)
        mail.setFrom("no-reply@suggest.io")
        mail.setRecipient(FEEDBACK_RCVR_EMAILS : _*)
        val personId = request.pwOpt.get.personId
        userEmailsFut.map { ues =>
          val username = ues.headOption getOrElse personId
          mail.setSubject("SiO Market: Вопрос от пользователя " + lsr.name.orElse(ues.headOption).getOrElse(""))
          mail.send(
            bodyText = views.txt.market.lk.support.emailSupportRequestedTpl(username, lsr, adnIdOpt, r = r)
          )
        } flatMap { _ =>
          // Письмо админам отправлено. Нужно куда-то перенаправить юзера.
          RdrBackOrFut(r) { Ident.redirectCallUserSomewhere(personId) }
            .map { rdr =>
              rdr.flashing("success" -> "Ваше сообщение отправлено.")
            }
        }
      }
    )
  }

}
