package controllers

import com.google.inject.Inject
import io.suggest.event.SioNotifierStaticClientI
import models._
import models.usr.MPersonIdent
import org.elasticsearch.client.Client
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import util.ident.{IIdentUtil, IdentUtil}
import util.{FormUtil, PlayLazyMacroLogsImpl}
import util.acl._
import util.mail.IMailerWrapper
import views.html.lk.support._
import scala.concurrent.{ExecutionContext, Future}
import util.support.SupportUtil
import play.api.data._, Forms._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.14 14:39
 * Description: Контроллер для обратной связи с техподдержкой s.io в личном кабинете узла.
 */
class MarketLkSupport @Inject() (
  override val messagesApi      : MessagesApi,
  override val mailer           : IMailerWrapper,
  override val identUtil        : IdentUtil,
  supportUtil                   : SupportUtil,
  override implicit val ec      : ExecutionContext,
  implicit val esClient         : Client,
  override implicit val sn      : SioNotifierStaticClientI
)
  extends SioController
  with PlayLazyMacroLogsImpl
  with IMailer
  with IIdentUtil
{

  import LOGGER._

  // TODO Объеденить node и не-node вызовы в единые экшены.
  // TODO Разрешить анонимусам слать запросы при наличии капчи в экшен-билдере.

  /** Маппинг для формы обращения в саппорт. */
  private def supportFormM = {
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
  def supportFormNode(adnId: String, r: Option[String]) = IsAdnNodeAdminGet(adnId).async { implicit request =>
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
  def supportFormNodeSubmit(adnId: String, r: Option[String]) = IsAdnNodeAdminPost(adnId).async { implicit request =>
    _supportFormSubmit(Some(request.adnNode), r)
  }

  /** Сабмит формы обращения за помощью вне узла. */
  def supportFormSubmit(r: Option[String]) = IsAuth.async { implicit request =>
    _supportFormSubmit(None, r)
  }

  private def _supportFormSubmit(nodeOpt: Option[MAdnNode], r: Option[String])(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    val adnIdOpt = nodeOpt.flatMap(_.id)
    lazy val logPrefix = s"supportFormSubmit($adnIdOpt): "
    supportFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind lk-feedback form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(supportFormTpl(nodeOpt, formWithErrors, r))
      },
      {lsr =>
        val userEmailsFut = MPersonIdent.findAllEmails(request.pwOpt.get.personId)
        trace(logPrefix + "Processing from ip=" + request.remoteAddress)
        val msg = mailer.instance
        msg.setReplyTo(lsr.replyEmail)
        msg.setFrom("no-reply@suggest.io")
        msg.setRecipients( supportUtil.FEEDBACK_RCVR_EMAILS : _* )
        val personId = request.pwOpt.get.personId
        userEmailsFut.map { ues =>
          val username = ues.headOption getOrElse personId
          msg.setSubject("S.io Market: Вопрос от пользователя " + lsr.name.orElse(ues.headOption).getOrElse(""))
          msg.setText( views.txt.lk.support.emailSupportRequestedTpl(username, lsr, adnIdOpt, r = r) )
          msg.send()
        } flatMap { _ =>
          // Письмо админам отправлено. Нужно куда-то перенаправить юзера.
          RdrBackOrFut(r) { identUtil.redirectCallUserSomewhere(personId) }
            .map { rdr =>
              rdr.flashing(FLASH.SUCCESS -> "Your.msg.sent")
            }
        }
      }
    )
  }


  /** Маппинг формы запроса выставления геолокации узла. */
  // TODO Шаблону event'а бывает нужно получить доступ к такому маппингу. Нужно продумать, как это сделать.
  // play-2.4 начинает переезд на DI, поэтому этот контроллер должен стать классом, а не статическим объектом.
  private def geoNodeFormM: Form[String] = {
    import FormUtil._
    Form(
      "info" -> nonEmptyText(minLength = 0, maxLength = 2048)
        .transform(strTrimSanitizeF andThen replaceEOLwithBR, strIdentityF)
    )
  }

  /** Сабмит формы запроса выставления географии узла. */
  def askGeo4NodeSubmit(adnId: String, r: Option[String]) = IsAdnNodeAdmin(adnId).async { implicit request =>
    lazy val logPrefix = s"addNodeGeoSubmit($adnId): "
    geoNodeFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("Failed to bind support form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable
      },
      {text =>
        val personId = request.pwOpt.get.personId
        val emailsFut = MPersonIdent.findAllEmails(personId)
        trace(logPrefix + "Processing from ip=" + request.remoteAddress)
        // собираем письмо админам s.io
        val msg = mailer.instance
        msg.setSubject("sio-market: Запрос геолокации для узла " + request.adnNode.meta.name + request.adnNode.meta.town.fold("")(" / " + _))
        msg.setRecipients( supportUtil.FEEDBACK_RCVR_EMAILS : _* )
        msg.setFrom("no-reply@suggest.io")
        emailsFut map { emails =>
          val emailOpt = emails.headOption
          if (emailOpt.isDefined)
            msg.setReplyTo(emailOpt.get)
          msg.setHtml( views.html.lk.support.emailGeoNodeRequestTpl(emails, request.adnNode, text) )
          msg.send()
        } flatMap { _ =>
          // Письмо отправлено админам. Нужно куда-то перенаправить юзера.
          RdrBackOrFut(r) { identUtil.redirectCallUserSomewhere(personId) }
            .map { rdr =>
              rdr.flashing(FLASH.SUCCESS -> "Your.req.sent")
            }
        }
      }
    )
  }

}
