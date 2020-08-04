package controllers

import io.suggest.n2.edge.MPredicates
import javax.inject.Inject
import io.suggest.n2.node.MNode
import io.suggest.util.logs.MacroLogsImplLazy
import models.mhelp.MLkSupportRequest
import models.req.{INodeReq, IReq, IReqHdr}
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.{ActionBuilder, AnyContent, Result}
import util.acl._
import util.ident.IdentUtil
import util.mail.IMailerWrapper
import util.support.SupportUtil
import views.html.lk.support._
import views.txt.lk.support.emailSupportRequestedTpl

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.14 14:39
 * Description: Контроллер для обратной связи с техподдержкой s.io в личном кабинете узла.
 */
final class LkHelp @Inject()(
                              sioControllerApi                : SioControllerApi,
                            )
  extends MacroLogsImplLazy
{

  import sioControllerApi._
  import mCommonDi.current.injector

  private lazy val mailer = injector.instanceOf[IMailerWrapper]
  private lazy val identUtil = injector.instanceOf[IdentUtil]
  private lazy val supportUtil = injector.instanceOf[SupportUtil]
  private lazy val bruteForceProtect = injector.instanceOf[BruteForceProtect]
  private lazy val maybeAuth = injector.instanceOf[MaybeAuth]
  private lazy val isAuth = injector.instanceOf[IsAuth]
  private lazy val isNodeAdmin = injector.instanceOf[IsNodeAdmin]

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
    isNodeAdmin(adnId, U.PersonNode, U.Lk).async { implicit request =>
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

  private def getEmails(nodeOptFut: Future[Option[MNode]]): Future[Iterable[String]] = {
    for {
      personNodeOpt <- nodeOptFut
    } yield {
      personNodeOpt
        .iterator
        .flatMap(_.edges.withPredicateIterIds( MPredicates.Ident.Email ))
        .toSeq
    }
  }

  private def _supportForm(nodeOpt: Option[MNode], r: Option[String])(implicit request: IReq[_]): Future[Result] = {
    // Взять дефолтовое значение email'а по сессии
    val emailsDfltFut = getEmails( request.user.personNodeOptFut )

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
        LOGGER.debug(logPrefix + "Failed to bind lk-feedback form:\n" + formatFormErrors(formWithErrors))
        _supportForm2(nodeOpt, formWithErrors, r, NotAcceptable)
      },

      {lsr =>
        val personId = request.user.personIdOpt.get
        val userEmailsFut = getEmails( request.user.personNodeOptFut )

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


  /** Страница "О компании" с какими-то данными юр.лица.
    *
    * @return Страница с инфой о компании.
    */
  def companyAbout(onNodeId: Option[String]) = csrf.AddToken {
    val actionBuilder = onNodeId.fold[ActionBuilder[IReq, AnyContent]]( maybeAuth() )( isNodeAdmin(_) )
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
