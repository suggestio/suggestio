package controllers.ident

import controllers._
import io.suggest.common.fut.FutureUtil
import io.suggest.init.routed.MJsiTgs
import io.suggest.util.logs.IMacroLogs
import models.mctx.{Context, CtxData}
import models.msession.Keys
import models.req.{IRecoverPwReq, IReq}
import models.usr._
import play.api.data._
import play.api.mvc.Result
import play.twirl.api.Html
import util.acl._
import util.di.IIdentUtil
import util.mail.IMailerWrapperDi
import util.xplay.SetLangCookieUtil
import views.html.helper.CSRF
import views.html.ident.mySioStartTpl
import views.html.ident.recover._

import scala.concurrent.Future
import models._
import util.FormUtil.passwordWithConfirmM
import util.secure.IScryptUtilDi

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
  with IMPersonIdents
  with IEmailPwIdentsDi
  with IEmailActivationsDi
{

  import mCommonDi._

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
      idents <- mPersonIdents.findIdentsByEmail(email1)
      if idents.nonEmpty

      epwIdent <- {
        val epwOpt0 = idents
          .foldLeft[List[EmailPwIdent]](Nil) {
            case (acc, epw: EmailPwIdent) => epw :: acc
            case (acc, _) => acc
          }
          .headOption

        FutureUtil.opt2future(epwOpt0) {
          val personId = idents.map(_.personId).head
          LOGGER.info(s"$logPrefix No ident exists, initializing new one for person[$personId]")
          val epw = EmailPwIdent(
            email       = email1,
            personId    = personId,
            pwHash      = "",
            isVerified  = false
          )
          for (_ <- emailPwIdents.save(epw)) yield {
            epw
          }
        }
      }

      eact = EmailActivation(email = email1, key = epwIdent.personId)
      eaId <- emailActivations.save(eact)

    } yield {

      val eact2 = eact.copy(
        id = Some(eaId)
      )
      // Можно отправлять письмецо на ящик.
      val msg = mailer.instance
      msg.setFrom("no-reply@suggest.io")
      msg.setRecipients(email1)
      val ctx = implicitly[Context]
      msg.setSubject("Suggest.io | " + ctx.messages("Password.recovery"))
      msg.setHtml {
        htmlCompressUtil.html4email {
          emailPwRecoverTpl(eact2)(ctx)
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
  with BruteForceProtect
  with SetLangCookieUtil
  with IIsAnonAcl
  with IIdentUtil
  with IMaybeAuth
  with EmailPwRegUtil
  with IEmailPwIdentsDi
  with IScryptUtilDi
{

  import mCommonDi._

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
      jsiTgs = MJsiTgs.CaptchaForm :: Nil
    )
    val ctx = implicitly[Context]
    val colHtml = _emailColTpl(form)(ctx)
    _outer(colHtml)(ctx)
  }

  /** Запрос страницы с формой вспоминания пароля по email'у. */
  def recoverPwForm = isAnon.Get { implicit request =>
    Ok(_recoverPwStep1(recoverPwFormM))
  }

  /** Сабмит формы восстановления пароля. */
  def recoverPwFormSubmit = isAnon.Post.async { implicit request =>
    bruteForceProtected {
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

  /** Рендер страницы, отображаемой когда запрос восстановления пароля принят.
    * CSRF используется, чтобы никому нельзя было слать ссылку с сообщением "ваш пароль выслан вам на почту". */
  def recoverPwAccepted(email1: String) = maybeAuth.Post() { implicit request =>
    val ctx = implicitly[Context]
    val colHtml = _acceptedColTpl(email1)(ctx)
    val html = _outer(colHtml)(ctx)
    Ok(html)
  }

  /** Форма сброса пароля. */
  private def pwResetFormM: PwResetForm_t = Form(passwordWithConfirmM)

  protected def _pwReset(form: PwResetForm_t)(implicit request: IRecoverPwReq[_]): Html = {
    val ctx = implicitly[Context]
    val colHtml = _pwResetColTpl(form, request.eact)(ctx)
    _outer(colHtml)(ctx)
  }

  /** Юзер перешел по ссылке восстановления пароля из письма. Ему нужна форма ввода нового пароля. */
  def recoverPwReturn(eActId: String) = canRecoverPw.Get(eActId)(_recoverKeyNotFound) { implicit request =>
    Ok(_pwReset(pwResetFormM))
  }

  /** Юзер сабмиттит форму с новым паролем. Нужно его залогинить, сохранить новый пароль в базу,
    * удалить запись из EmailActivation и отредиректить куда-нибудь. */
  def pwResetSubmit(eActId: String) = canRecoverPw.Post(eActId, U.PersonNode)(_recoverKeyNotFound).async { implicit request =>
    pwResetFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"pwResetSubmit($eActId): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        NotAcceptable(_pwReset(formWithErrors))
      },
      {newPw =>
        val pwHash2 = scryptUtil.mkHash(newPw)
        val epw2 = request.epw.copy(pwHash = pwHash2, isVerified = true)
        for {
          // Сохранение новых данных по паролю
          _         <- emailPwIdents.save(epw2)

          // Запуск удаления eact
          updateFut = emailActivations.deleteById(eActId)

          // Подготовить редирект
          rdr       <- identUtil.redirectUserSomewhere(epw2.personId)

          // Генерить ответ как только появляется возможность.
          res1      <- {
            val res0 = rdr
              .addingToSession(Keys.PersonId.name -> epw2.personId)
              .flashing(FLASH.SUCCESS -> "New.password.saved")
            setLangCookie2(res0, request.user.personNodeOptFut)
          }

          // Дожидаться успешного завершения асинхронных операций
          _         <- updateFut
        } yield {
          res1
        }
      }
    )
  }

}


