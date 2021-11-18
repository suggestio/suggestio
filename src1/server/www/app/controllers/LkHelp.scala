package controllers

import io.suggest.n2.edge.MPredicates

import javax.inject.Inject
import io.suggest.n2.node.MNode
import io.suggest.sec.util.Csrf
import io.suggest.util.logs.MacroLogsImplLazy
import models.mhelp.MLkSupportRequest
import models.req.{INodeReq, IReqHdr}
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.Result
import util.acl._
import util.ident.IdentUtil
import util.mail.IMailerWrapper
import util.support.SupportUtil
import util.tpl.HtmlCompressUtil
import views.html.lk.support._
import views.txt.lk.support._

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

  private lazy val mailer = injector.instanceOf[IMailerWrapper]
  private lazy val identUtil = injector.instanceOf[IdentUtil]
  private lazy val supportUtil = injector.instanceOf[SupportUtil]
  private lazy val bruteForceProtect = injector.instanceOf[BruteForceProtect]
  private lazy val maybeAuthMaybeNode = injector.instanceOf[MaybeAuthMaybeNode]
  private lazy val htmlCompressUtil = injector.instanceOf[HtmlCompressUtil]
  private lazy val csrf = injector.instanceOf[Csrf]
  private lazy val lkOfferoTpl = injector.instanceOf[LkOfferoTpl]

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
   * Отрендерить форму запроса помощи вне узла.
    *
    * @param r Адрес для возврата.
   * @return 200 Ok и страница с формой.
   */
  def supportForm(nodeIdOpt: Option[String], r: Option[String]) = csrf.AddToken {
    maybeAuthMaybeNode( nodeIdOpt, U.PersonNode, U.Lk ).async { implicit request =>
      // Взять дефолтовое значение email'а по сессии
      val emailsDfltFut = getEmails( request.user.personNodeOptFut )

      emailsDfltFut.flatMap { emailsDflt =>
        val emailDflt = emailsDflt.headOption.getOrElse("")
        val lsr = MLkSupportRequest(name = None, replyEmail = emailDflt, msg = "")
        val form = supportFormM.fill(lsr)

        _supportForm2( request.mnodeOpt, form, r, Ok )
      }
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


  private def _supportForm2(nodeOpt: Option[MNode], form: Form[MLkSupportRequest], r: Option[String], rs: Status)
                           (implicit request: IReqHdr): Future[Result] = {
    request.user.lkCtxDataFut.map { implicit ctxData =>
      rs( supportFormTpl(nodeOpt, form, r) )
    }
  }


  /** Сабмит формы обращения за помощью вне узла. */
  def supportFormSubmit(nodeIdOpt: Option[String], r: Option[String]) = csrf.Check {
    bruteForceProtect {
      maybeAuthMaybeNode( nodeIdOpt, U.PersonNode ).async { implicit request =>
        val adnIdOpt = request.mnodeOpt.flatMap(_.id)
        lazy val logPrefix = s"supportFormSubmit($adnIdOpt): "
        supportFormM.bindFromRequest().fold(
          {formWithErrors =>
            LOGGER.debug(logPrefix + "Failed to bind lk-feedback form:\n" + formatFormErrors(formWithErrors))
            _supportForm2( request.mnodeOpt, formWithErrors, r, NotAcceptable)
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
    }
  }


  /** Страница "О компании" с какими-то данными юр.лица.
    *
    * @return Страница с инфой о компании.
    */
  def companyAbout(onNodeId: Option[String]) = csrf.AddToken {
    maybeAuthMaybeNode( onNodeId, U.Lk ).async { implicit request =>
      val mnodeOpt = request match {
        case nreq: INodeReq[_] =>
          Some(nreq.mnode)
        case _ =>
          None
      }

      request.user.lkCtxDataFut.map { implicit ctxData =>
        val html = companyAboutTpl(
          nodeOpt = mnodeOpt
        )
        Ok(html)
          .cacheControl( 3600 )
      }
    }
  }


  /** User agreement page. */
  def offero(onNodeId: Option[String]) = csrf.AddToken {
    maybeAuthMaybeNode( onNodeId, U.Lk ).async { implicit request =>
      request.user.lkCtxDataFut.map { implicit ctxData =>
        Ok( lkOfferoTpl( request.mnodeOpt ) )
          .cacheControl(3600)
      }
    }
  }

}
