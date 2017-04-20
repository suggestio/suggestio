package controllers

import com.google.inject.Inject
import io.suggest.util.logs.MacroLogsImplLazy
import models._
import models.mhelp.MLkSupportRequest
import models.mproj.ICommonDi
import models.req.{INodeReq, IReq, IReqHdr}
import models.usr.MPersonIdents
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.{ActionBuilder, Result}
import util.acl._
import util.ident.IdentUtil
import util.mail.IMailerWrapper
import util.support.SupportUtil
import util.FormUtil
import views.html.lk.support._
import views.txt.lk.support.emailSupportRequestedTpl

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.14 14:39
 * Description: Контроллер для обратной связи с техподдержкой s.io в личном кабинете узла.
 */
class LkHelp @Inject()(
                                  mailer                          : IMailerWrapper,
                                  identUtil                       : IdentUtil,
                                  mPersonIdents                   : MPersonIdents,
                                  supportUtil                     : SupportUtil,
                                  bruteForceProtect               : BruteForceProtect,
                                  isAuth                          : IsAuth,
                                  isNodeAdmin                     : IsNodeAdmin,
                                  override val mCommonDi          : ICommonDi
)
  extends SioController
  with MacroLogsImplLazy
{

  import LOGGER._
  import mCommonDi._

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
    *
    * @return 200 Ок и страница с формой.
   */
  def supportFormNode(adnId: String, r: Option[String]) = csrf.AddToken {
    isNodeAdmin(adnId, U.Lk).async { implicit request =>
      val mnodeOpt = Some(request.mnode)
      _supportForm(mnodeOpt, r)
    }
  }

  /**
   * Отрендерить форму запроса помощи вне узла.
    *
    * @param r Адрес для возврата.
   * @return 200 Ok и страница с формой.
   */
  def supportForm(r: Option[String]) = csrf.AddToken {
    isAuth().async { implicit request =>
      _supportForm(None, r)
    }
  }

  private def _supportForm(nodeOpt: Option[MNode], r: Option[String])(implicit request: IReq[_]): Future[Result] = {
    // Взять дефолтовое значение email'а по сессии
    val emailsDfltFut = request.user
      .personIdOpt
      .fold [Future[Seq[String]]] {
        Future.successful( Nil )
      } { personId =>
        mPersonIdents.findAllEmails(personId)
      }
    emailsDfltFut.flatMap { emailsDflt =>
      val emailDflt = emailsDflt.headOption.getOrElse("")
      val lsr = MLkSupportRequest(name = None, replyEmail = emailDflt, msg = "")
      val form = supportFormM.fill(lsr)

      _supportForm2(nodeOpt, form, r, Ok)
    }
  }

  private def _supportForm2(nodeOpt: Option[MNode], form: Form[MLkSupportRequest], r: Option[String], rs: Status)
                           (implicit request: IReqHdr): Future[Result] = {
    request.user.lkCtxDataFut.map { implicit ctxData =>
      rs( supportFormTpl(nodeOpt, form, r) )
    }
  }

  /** Сабмит формы обращения за помощью по узлу, которым управляем. */
  def supportFormNodeSubmit(adnId: String, r: Option[String]) = csrf.Check {
    bruteForceProtect {
      isNodeAdmin(adnId).async { implicit request =>
        val mnodeOpt = Some(request.mnode)
        _supportFormSubmit(mnodeOpt, r)
      }
    }
  }

  /** Сабмит формы обращения за помощью вне узла. */
  def supportFormSubmit(r: Option[String]) = csrf.Check {
    bruteForceProtect {
      isAuth().async { implicit request =>
        _supportFormSubmit(None, r)
      }
    }
  }

  private def _supportFormSubmit(nodeOpt: Option[MNode], r: Option[String])(implicit request: IReq[_]): Future[Result] = {
    val adnIdOpt = nodeOpt.flatMap(_.id)
    lazy val logPrefix = s"supportFormSubmit($adnIdOpt): "
    supportFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind lk-feedback form:\n" + formatFormErrors(formWithErrors))
        _supportForm2(nodeOpt, formWithErrors, r, NotAcceptable)
      },

      {lsr =>
        val personId = request.user.personIdOpt.get
        val userEmailsFut = mPersonIdents.findAllEmails(personId)
        trace(logPrefix + "Processing from ip=" + request.remoteAddress)

        val msg = mailer.instance
        msg.setReplyTo(lsr.replyEmail)
        msg.setRecipients( supportUtil.FEEDBACK_RCVR_EMAILS : _* )

        for {
          ues <- userEmailsFut
          rdrFut = RdrBackOrFut(r) { identUtil.redirectCallUserSomewhere(personId) }
          rdr <- {
            val username = ues.headOption.getOrElse( personId )
            msg.setSubject("S.io Market: Вопрос от пользователя " + lsr.name.orElse(ues.headOption).getOrElse(""))
            msg.setText {
              htmlCompressUtil.txt2str {
                emailSupportRequestedTpl(username, lsr, adnIdOpt, r = r)
              }
            }
            msg.send()

            rdrFut
          }
        } yield {
          rdr.flashing(FLASH.SUCCESS -> "Your.msg.sent")
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
  def askGeo4NodeSubmit(adnId: String, r: Option[String]) = csrf.Check {
    isNodeAdmin(adnId).async { implicit request =>
      lazy val logPrefix = s"addNodeGeoSubmit($adnId): "

      geoNodeFormM.bindFromRequest().fold(
        {formWithErrors =>
          debug("Failed to bind support form:\n" + formatFormErrors(formWithErrors))
          NotAcceptable
        },
        {text =>
          val personId = request.user.personIdOpt.get

          val emailsFut = mPersonIdents.findAllEmails(personId)
          trace(logPrefix + "Processing from ip=" + request.remoteAddress)
          // собираем письмо админам s.io

          val msg = mailer.instance
          val mnode = request.mnode

          msg.setSubject(
            "sio-market: Запрос геолокации для узла " +
              mnode.meta.basic.name +
              mnode.meta.address.town.fold("")(" / " + _)
          )

          msg.setRecipients( supportUtil.FEEDBACK_RCVR_EMAILS : _* )

          for {
            emails <- emailsFut
            // Организовать редирект.
            rdrFut = RdrBackOrFut(r) { identUtil.redirectCallUserSomewhere(personId) }
            rdr    <- {
              // Отправить сообщение.
              val emailOpt = emails.headOption
              if (emailOpt.isDefined)
                msg.setReplyTo(emailOpt.get)
              msg.setHtml {
                htmlCompressUtil.html4email {
                  emailGeoNodeRequestTpl(emails, mnode, text)
                }
              }
              msg.send()

              // Вернуть фьючерс редиректа.
              rdrFut
            }
          } yield {
            rdr.flashing(FLASH.SUCCESS -> "Your.req.sent")
          }
        }
      )
    }
  }


  /** Страница "О компании" с какими-то данными юр.лица.
    *
    * @return Страница с инфой о компании.
    */
  def companyAbout(onNodeId: Option[String]) = csrf.AddToken {
    val actionBuilder = onNodeId.fold[ActionBuilder[IReq]]( isAuth() )( isNodeAdmin(_) )
    actionBuilder { implicit request =>
      val mnodeOpt = request match {
        case nreq: INodeReq[_] =>
          Some(nreq.mnode)
        case _ =>
          None
      }

      val html = companyAboutTpl(
        nodeOpt = mnodeOpt
      )
      Ok(html)
        .withHeaders(CACHE_CONTROL -> "public, max-age=3600")
    }
  }

}