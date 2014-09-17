package controllers

import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Result
import util.event.SiowebNotifier.Implicts.sn
import util.SiowebEsUtil.client
import util.PlayLazyMacroLogsImpl
import util.acl.{IsAdnNodeAdmin, AbstractRequestWithPwOpt, IsAuth}
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
  private def supportFormM = {
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


  /**
   * Отрендерить форму с запросом помощи с узла.
   * @return 200 Ок и страница с формой.
   */
  def supportFormNode(adnId: String, r: Option[String]) = IsAdnNodeAdmin(adnId).async { implicit request =>
    _supportForm(Some(request.adnNode), r)
  }

  /**
   * Отрендерить форму запроса помощи вне узла.
   * @param r Адрес для возврата.
   * @return 200 Ok и страница с формой.
   */
  def supportForm(r: Option[String]) = IsAuth.async { implicit request =>
    _supportForm(None, r)
  }

  private def _supportForm(nodeOpt: Option[MAdnNode], r: Option[String])(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    // Взять дефолтовое значение email'а по сессии
    val emailsDfltFut = request.pwOpt.fold [Future[Seq[String]]]
      { Future successful Nil }
      { pw => MPersonIdent.findAllEmails(pw.personId) }
    for {
      emailsDflt <- emailsDfltFut
    } yield {
      val emailDflt = emailsDflt.headOption getOrElse ""
      val lsr = MLkSupportRequest(name = None, replyEmail = emailDflt, msg = "")
      val form = supportFormM fill lsr
      Ok(supportFormTpl(nodeOpt, form, r))
    }
  }


  /** Сабмит формы обращения за помощью по узлу, которым управляем. */
  def supportFormNodeSubmit(adnId: String, r: Option[String]) = IsAdnNodeAdmin(adnId).async { implicit request =>
    _supportFormSubmit(Some(request.adnNode), r)
  }

  /** Сабмит формы обращения за помощью вне узла. */
  def supportFormSubmit(r: Option[String]) = IsAuth.async { implicit request =>
    _supportFormSubmit(None, r)
  }

  private def _supportFormSubmit(nodeOpt: Option[MAdnNode], r: Option[String])(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    lazy val adnIdOpt = nodeOpt.flatMap(_.id)
    lazy val logPrefix = s"supportFormSubmit($adnIdOpt): "
    supportFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind lk-feedback form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(supportFormTpl(nodeOpt, formWithErrors, r))
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
