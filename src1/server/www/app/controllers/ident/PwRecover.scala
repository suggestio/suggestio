package controllers.ident

import controllers._
import io.suggest.ctx.CtxData
import io.suggest.es.model.EsModelDi
import io.suggest.i18n.MsgCodes
import io.suggest.init.routed.MJsInitTargets
import io.suggest.model.n2.edge.{MEdge, MEdgeInfo, MNodeEdges, MPredicates}
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.{IMNodes, MNode, MNodeTypes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.sec.m.msession.Keys
import io.suggest.sec.util.IScryptUtilDi
import io.suggest.util.logs.IMacroLogs
import models.mctx.Context
import models.req.IReq
import models.usr._
import play.api.data._
import play.api.mvc.Result
import play.twirl.api.Html
import util.acl._
import util.mail.IMailerWrapperDi
import util.xplay.SetLangCookieUtil
import views.html.helper.CSRF
import views.html.ident.mySioStartTpl
import views.html.ident.recover._
import japgolly.univeq._

import scala.concurrent.Future
import models._
import util.FormUtil.passwordWithConfirmM
import util.ident.IIdentUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 16:23
 * Description: Аддон для Ident-контроллера для поддержки восстановления пароля.
 */


/** Хелпер контроллеров, занимающийся отправкой почты для восстановления пароля. */
trait SendPwRecoverEmail
  extends SioController
  with IMailerWrapperDi
  with IMacroLogs
  with IBruteForceProtect
  with IMNodes
  with EsModelDi
{

  import mCommonDi._
  import esModel.api._

  /**
   * Отправка письма юзеру. Это статический метод, но он сильно завязан на внутренности sio-контроллеров,
   * поэтому он реализован как аддон для контроллеров.
   * @param email1 email юзера.
   * @return Фьючерс для синхронизации.
   */
  protected def sendRecoverMail(email1: String)(implicit request: IReq[_]): Future[_] = {
    lazy val logPrefix = s"sendRecoverMail($email1):"

    val fut = for {
      // Надо найти юзера в базах PersonIdent, и если есть, то отправить письмецо.
      hasIdentNodes <- mNodes.dynExists {
        new MNodeSearchDfltImpl {
          override val nodeTypes = MNodeTypes.Person :: Nil
          override val outEdges: Seq[Criteria] = {
            val cr = Criteria(
              predicates = MPredicates.Ident.Email :: Nil,
              nodeIds    = email1 :: Nil
            )
            cr :: Nil
          }
          // Наврядли будет больше одного ответа. (Не должно бы быть, по логике).
          override def limit = 10
        }
      }

      if hasIdentNodes

    } yield {

      // Можно отправлять письмецо на ящик.
      val msg = mailer.instance
      msg.setRecipients(email1)
      val ctx = implicitly[Context]
      msg.setSubject( ctx.messages("Password.recovery") + " | " + MsgCodes.`Suggest.io` )
      val qs = MEmailRecoverQs( email1 )
      msg.setHtml {
        htmlCompressUtil.html4email {
          emailPwRecoverTpl(qs)(ctx)
        }
      }
      msg.send()
    }

    // Отрабатываем ситуацию, когда юзера нет совсем.
    fut.recover { case _: NoSuchElementException =>
      // TODO Если юзера нет, то создать его и тоже отправить письмецо с активацией? или что-то иное вывести?
      LOGGER.warn(s"$logPrefix No email idents found for recovery")
      // None вместо Unit(), чтобы 2.11 компилятор не ругался.
      None
    }

  }

}


trait PwRecover
  extends SendPwRecoverEmail
  with IMacroLogs
  with CaptchaValidator
  with IBruteForceProtect
  with SetLangCookieUtil
  with IIsAnonAcl
  with IIdentUtil
  with IMaybeAuth
  with EmailPwRegUtil
  with IScryptUtilDi
  with EsModelDi
{

  import mCommonDi._
  import esModel.api._

  val canRecoverPw: CanRecoverPw

  /** Маппинг формы восстановления пароля. */
  private def recoverPwFormM: EmailPwRecoverForm_t = {
    emailRegFormM
  }

  private def _recoverKeyNotFound(req: IReq[_]): Future[Result] = {
    implicit val req1 = req
    NotFound( failedColTpl() )
  }

  // TODO Сделать это шаблоном!
  protected def _outer(html: Html)(implicit ctx: Context): Html = {
    mySioStartTpl(
      title     = ctx.messages("Password.recovery"),
      columns   = Seq(html)
    )(ctx)
  }

  /** Рендер содержимого страницы с формой восстановления пароля. */
  protected def _recoverPwStep1(form: EmailPwRecoverForm_t)(implicit request: IReq[_]): Html = {
    implicit val ctxData = CtxData(
      jsInitTargets = MJsInitTargets.CaptchaForm :: Nil
    )
    val ctx = implicitly[Context]
    val colHtml = _emailColTpl(form)(ctx)
    _outer(colHtml)(ctx)
  }

  /** Запрос страницы с формой вспоминания пароля по email'у. */
  def recoverPwForm = csrf.AddToken {
    isAnon() { implicit request =>
      Ok(_recoverPwStep1(recoverPwFormM))
    }
  }

  /** Сабмит формы восстановления пароля. */
  def recoverPwFormSubmit = csrf.Check {
    bruteForceProtect {
      isAnon().async { implicit request =>
        val formBinded = checkCaptcha(recoverPwFormM.bindFromRequest())
        formBinded.fold(
          {formWithErrors =>
            LOGGER.debug("recoverPwFormSubmit(): Failed to bind form:\n" + formatFormErrors(formWithErrors))
            NotAcceptable(_recoverPwStep1(formWithErrors))
          },
          {email1 =>
            sendRecoverMail(email1) map { _ =>
              // отрендерить юзеру результат, что всё ок, независимо от успеха поиска.
              rmCaptcha(formBinded){
                Redirect( CSRF(routes.Ident.recoverPwAccepted(email1)) )
              }
            }
          }
        )
      }
    }
  }

  /** Рендер страницы, отображаемой когда запрос восстановления пароля принят.
    * CSRF используется, чтобы никому нельзя было слать ссылку с сообщением "ваш пароль выслан вам на почту". */
  def recoverPwAccepted(email1: String) = csrf.Check {
    maybeAuth() { implicit request =>
      val ctx = implicitly[Context]
      val colHtml = _acceptedColTpl(email1)(ctx)
      val html = _outer(colHtml)(ctx)
      Ok(html)
    }
  }

  /** Форма сброса пароля. */
  private def pwResetFormM: PwResetForm_t = Form(passwordWithConfirmM)

  protected def _pwReset(form: PwResetForm_t, qs: MEmailRecoverQs)(implicit request: IReq[_]): Html = {
    val ctx = implicitly[Context]
    val colHtml = _pwResetColTpl(form, qs)(ctx)
    _outer(colHtml)(ctx)
  }


  /** Юзер перешел по ссылке восстановления пароля из письма. Ему нужна форма ввода нового пароля. */
  def recoverPwReturn(qs: MEmailRecoverQs) = csrf.AddToken {
    canRecoverPw(qs)(_recoverKeyNotFound) { implicit request =>
      Ok(_pwReset(pwResetFormM, qs))
    }
  }


  /** Юзер сабмиттит форму с новым паролем. Нужно его залогинить, сохранить новый пароль в базу,
    * удалить запись из EmailActivation и отредиректить куда-нибудь. */
  def pwResetSubmit(qs: MEmailRecoverQs) = csrf.Check {
    bruteForceProtect {
      canRecoverPw(qs, U.PersonNode)(_recoverKeyNotFound).async { implicit request =>
        pwResetFormM.bindFromRequest().fold(
          {formWithErrors =>
            LOGGER.debug(s"pwResetSubmit(${qs.email}): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
            NotAcceptable(_pwReset(formWithErrors, qs))
          },
          {newPw =>
            val pwHash2 = scryptUtil.mkHash(newPw)

            val updatedNodeFut = mNodes.tryUpdate( request.mnode ) { mnode0 =>
              MNode.edges
                .composeLens( MNodeEdges.out )
                .modify { edges0 =>
                  val edges1 = edges0
                    .iterator
                    .flatMap { e0 =>
                      if (
                        (e0.predicate ==* MPredicates.Ident.Email) &&
                          (e0.nodeIds contains qs.email) &&
                          !e0.info.flag.contains(true)
                      ) {
                        // Надо выставить флаг, что почта выверена:
                        val e2 = MEdge.info
                          .composeLens( MEdgeInfo.flag )
                          .set( Some(true) )( e0 )
                        e2 :: Nil
                      } else if (e0.predicate ==* MPredicates.Ident.Password) {
                        Nil
                      } else {
                        e0 :: Nil
                      }
                    }
                    .toStream

                  // Добавить парольный эдж:
                  val pwEdge = MEdge(
                    predicate = MPredicates.Ident.Password,
                    info = MEdgeInfo(
                      textNi = Some(pwHash2)
                    )
                  )
                  val edges2 = pwEdge #:: edges1

                  MNodeEdges.edgesToMap1( edges2 )

                }( mnode0 )
            }

            for {
              // Сохранение новых данных по паролю
              _         <- updatedNodeFut

              personId = request.mnode.id.get

              // Подготовить редирект
              rdr       <- identUtil.redirectUserSomewhere( personId )

              // Генерить ответ как только появляется возможность.
              res1      <- {
                val res0 = rdr
                  .addingToSession(Keys.PersonId.value -> personId)
                  .flashing(FLASH.SUCCESS -> "New.password.saved")
                setLangCookie2(res0, request.user.personNodeOptFut)
              }

            } yield {
              res1
            }
          }
        )
      }
    }
  }

}


