package controllers

import play.api.Play.current
import play.api.libs.ws._
import play.api.data._
import play.api.data.Forms._
import util.acl._
import util._
import play.api.mvc._
import views.html.ident._, recover._
import play.api.libs.concurrent.Promise.timeout
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import scala.concurrent.Future
import play.api.libs.json.JsString
import models._
import play.api.mvc.Security.username
import play.api.i18n.{Messages, Lang}
import SiowebEsUtil.client
import scala.util.{Failure, Success}
import com.typesafe.scalalogging.slf4j.Logger
import com.typesafe.plugin.{use, MailerPlugin}
import util.acl.PersonWrapper.PwOpt_t
import FormUtil.{passwordM, passwordWithConfirmM}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 11:47
 * Description: Контроллер обычного логина в систему. Обычно логинятся через Mozilla Persona, но не исключено, что
 * в будущем будет также и вход по имени/паролю для некоторых учетных записей.
 */

object Ident extends SioController with PlayMacroLogsImpl with EmailPwSubmit with BruteForceProtect
  with CaptchaValidator with ChangePwAction {

  import LOGGER._

  // URL, используемый для person'a. Если сие запущено на локалхосте, то надо менять этот адресок.
  val AUDIENCE_URL = current.configuration.getString("persona.audience.url").get

  type EmailPwLoginForm_t = Form[(String, String)]

  /** Форма логина по email и паролю. */
  val emailPwLoginFormM: EmailPwLoginForm_t = Form(tuple(
    "email"    -> email,
    "password" -> passwordM
  ))

  /**
   * Юзер разлогинивается. Выпилить из сессии данные о его логине.
   * @return Редирект на главную, ибо анонимусу идти больше некуда.
   */
  def logout = Action { implicit request =>
    Redirect(routes.Application.index())
      .withNewSession
  }


  /** Рендер страницы с возможностью логина по email и паролю. */
  def emailPwLoginForm(r: Option[String]) = IsAnon { implicit request =>
    Ok(emailPwLoginFormTpl(emailPwLoginFormM, r))
  }


  override def emailSubmitError(lf: EmailPwLoginForm_t, r: Option[String])(implicit request: AbstractRequestWithPwOpt[_]): Future[Result] = {
    Forbidden(emailPwLoginFormTpl(lf, r))
  }


  // TODO выставить нормальный routing тут
  protected def rdrToAdmin = Redirect(routes.Application.index())


  // Восстановление пароля

  private val recoverPwFormM = Form(
    mapping(
      "email" -> email,
      CAPTCHA_ID_FN    -> Captcha.captchaIdM,
      CAPTCHA_TYPED_FN -> Captcha.captchaTypedM
    )
    {(email1, _, _) => email1 }
    {email1 => Some((email1, "", ""))}
  )

  /** Запрос страницы с формой вспоминания пароля по email'у. */
  def recoverPwForm = IsAnon { implicit request =>
    Ok(recoverPwFormTpl(recoverPwFormM))
  }

  /** Сабмит формы восстановления пароля. */
  def recoverPwFormSubmit = IsAnon.async { implicit request =>
    bruteForceProtected {
      val formBinded = checkCaptcha(recoverPwFormM.bindFromRequest())
      formBinded.fold(
        {formWithErrors =>
          debug("recoverPwFormSubmit(): Failed to bind form:\n" + formatFormErrors(formWithErrors))
          NotAcceptable(recoverPwFormTpl(formWithErrors))
        },
        {email1 =>
          // TODO Надо найти юзера в базах EmailPwIdent и MozPersonaIdent, и если есть, то отправить письмецо.
          MPersonIdent.findIdentsByEmail(email1) flatMap { idents =>
            if (idents.nonEmpty) {
              val emailIdentFut: Future[EmailPwIdent] = idents
                .foldLeft[List[EmailPwIdent]](Nil) {
                case (acc, epw: EmailPwIdent) => epw :: acc
                case (acc, _) => acc
              }
                .headOption
                .map { Future successful }
                .getOrElse {
                  // берём personId из moz persona. Там в списке только один элемент, т.к. email является уникальным в рамках ident-модели.
                  val personId = idents.map(_.personId).head
                  val epw = EmailPwIdent(email = email1, personId = personId, pwHash = "", isVerified = false)
                  epw.save
                    .map { _ => epw}
                }
              emailIdentFut flatMap { epwIdent =>
                // Нужно сгенерить ключ для восстановления пароля. И ссылку к нему.
                val eact = EmailActivation(email = email1, key = epwIdent.personId)
                eact.save.map { eaId =>
                  val eact2 = eact.copy(
                    id = Some(eaId)
                  )
                  // Можно отправлять письмецо на ящик.
                  val mail = use[MailerPlugin].email
                  mail.setFrom("no-reply@suggest.io")
                  mail.setRecipient(email1)
                  val ctx = implicitly[Context]
                  mail.setSubject("Suggest.io | " + Messages("Password.recovery")(ctx.lang))
                  mail.send(
                    bodyText = views.txt.ident.recover.emailPwRecoverTpl(eact2)(ctx),
                    bodyHtml = emailPwRecoverTpl(eact2)(ctx)
                  )
                }
              }
            } else {
              // TODO Если юзера нет, то создать его и тоже отправить письмецо с активацией? или что-то иное вывести?
              Future successful()
            }
          } map { _ =>
            // отрендерить юзеру результат, что всё ок, независимо от успеха поиска.
            rmCaptcha(formBinded){
              Redirect(routes.Ident.recoverPwAccepted(email1))
            }
          }
        }
      )
    }
  }

  /** Рендер страницы, отображаемой когда запрос восстановления пароля принят. */
  def recoverPwAccepted(email1: String) = MaybeAuth { implicit request =>
    Ok(pwRecoverAcceptedTpl(email1))
  }


  /**
   * ActionBuilder для некоторых экшенов восстановления пароля. Завязан на некоторые фунции контроллера, поэтому
   * лежит здесь.
   * @param eActId id ключа активации.
   */
  case class CanRecoverPw(eActId: String) extends ActionBuilder[RecoverPwRequest] with PlayMacroLogsImpl {
    protected def keyNotFound(implicit request: RequestHeader): Future[Result] = {
      MarketIndexAccess.getNodes.map { nodes =>
        implicit val ctx = new ContextImpl
        NotFound(pwRecoverFailedTpl(nodes))
      }
    }
    override def invokeBlock[A](request: Request[A], block: (RecoverPwRequest[A]) => Future[Result]): Future[Result] = {
      lazy val logPrefix = s"CanRecoverPw($eActId): "
      bruteForceProtectedNoimpl(request) {
        val pwOpt = PersonWrapper.getFromRequest(request)
        val srmFut = SioReqMd.fromPwOpt(pwOpt)
        EmailActivation.getById(eActId).flatMap {
          case Some(eAct) =>
            EmailPwIdent.getById(eAct.email) flatMap {
              case Some(epw) if epw.personId == eAct.key =>
                // Можно отрендерить блок
                debug(logPrefix + "ok: " + request.path)
                srmFut flatMap { srm =>
                  val req1 = RecoverPwRequest(request, pwOpt, epw, eAct, srm)
                  block(req1)
                }
              // should never occur: Почему-то нет парольной записи для активатора.
              // Такое возможно, если юзер взял ключ инвайта в маркет и вставил его в качестве ключа восстановления пароля.
              case None =>
                error(s"${logPrefix}eAct exists, but emailPw is NOT! Hacker? pwOpt = $pwOpt ;; eAct = $eAct")
                keyNotFound(request)
            }
          case None =>
            pwOpt match {
              // Вероятно, юзер повторно перешел по ссылке из письма.
              case Some(pw) =>
                redirectUserSomewhere(pw.personId)
              // Юзер неизвестен и ключ неизвестен. Возможно, перебор ключей какой-то?
              case None =>
                warn(logPrefix + "Unknown eAct key. pwOpt = " + pwOpt)
                keyNotFound(request)
            }
        }
      }
    }
  }

  /** Реквест активации. */
  case class RecoverPwRequest[A](request: Request[A], pwOpt: PwOpt_t, epw: EmailPwIdent, eAct: EmailActivation, sioReqMd: SioReqMd)
    extends AbstractRequestWithPwOpt(request)


  /** Форма сброса пароля. */
  private val pwResetFormM = Form(passwordWithConfirmM)

  /** Юзер перешел по ссылке восстановления пароля из письма. Ему нужна форма ввода нового пароля. */
  def recoverPwReturn(eActId: String) = CanRecoverPw(eActId).async { implicit request =>
    MarketIndexAccess.getNodes map { nodes =>
      Ok(pwResetTpl(request.eAct, pwResetFormM, nodes))
    }
  }

  /** Юзер сабмиттит форму с новым паролем. Нужно его залогинить, сохранить новый пароль в базу,
    * удалить запись из EmailActivation и отредиректить куда-нибудь. */
  def pwResetSubmit(eActId: String) = CanRecoverPw(eActId).async { implicit request =>
    pwResetFormM.bindFromRequest().fold(
      {formWithErrors =>
        val nodesFut = MarketIndexAccess.getNodes
        debug(s"pwResetSubmit($eActId): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
        nodesFut map { nodes =>
          NotAcceptable(pwResetTpl(request.eAct, formWithErrors, nodes))
        }
      },
      {newPw =>
        val pwHash2 = MPersonIdent.mkHash(newPw)
        val epw2 = request.epw.copy(pwHash = pwHash2, isVerified = true)
        for {
          _   <- epw2.save
          _   <- request.eAct.delete
          rdr <- redirectUserSomewhere(epw2.personId)
        } yield {
          rdr.withSession(username -> epw2.personId)
            .flashing("success" -> "Новый пароль сохранён.")
        }
      }
    )
  }

  def rdrUserSomewhere = IsAuth.async { implicit request =>
    redirectUserSomewhere(request.pwOpt.get.personId)
  }

  def redirectCallUserSomewhere(personId: String): Future[Call] = {
    MarketLk.getMarketRdrCallFor(personId) map {
      _ getOrElse routes.Market.index() // Был раньше Admin.index(), но кравлер пока выпилен ведь.
    }
  }

  /** Сгенерить редирект куда-нибудь для указанного юзера. */
  def redirectUserSomewhere(personId: String): Future[Result] = {
    redirectCallUserSomewhere(personId) map { Redirect }
  }


  /** Маппинг формы смены пароля. */
  def changePasswordFormM = Form(tuple(
    "old" -> passwordM,
    "new" -> passwordWithConfirmM
  ))

  /** Страница с формой смены пароля. */
  def changePassword = IsAuth { implicit request =>
    Ok(changePasswordTpl(changePasswordFormM))
  }

  def changePasswordSubmit(r: Option[String]) = IsAuth.async { implicit request =>
    _changePasswordSubmit(r) { formWithErrors =>
      NotAcceptable(changePasswordTpl(formWithErrors))
    }
  }
}


/** Контексто-зависимый код экшена, который реализует смену пароля у пользователя.
  * Реализации должны оборачивать логику экшена в экшен, выставляя обработчики для ошибок и успехов. */
trait ChangePwAction extends SioController with PlayMacroLogsI {
  import Ident.changePasswordFormM

  /** Если неясно куда надо редиректить юзера, то что делать? */
  def changePwOkRdrDflt(implicit request: AbstractRequestWithPwOpt[AnyContent]): Future[Call] = {
    // TODO Избавится от get, редиректя куда-нить в другое место.
    Ident.redirectCallUserSomewhere(request.pwOpt.get.personId)
  }

  /** Сабмит формы смены пароля. Нужно проверить старый пароль и затем заменить его новым. */
  def _changePasswordSubmit(r: Option[String])(onError: Form[(String, String)] => Future[Result])(implicit request: AbstractRequestWithPwOpt[AnyContent]): Future[Result] = {
    changePasswordFormM.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug("changePasswordSubmit(): Failed to bind form:\n" + formatFormErrors(formWithErrors))
        onError(formWithErrors)
      },
      {case (oldPw, newPw) =>
        // Нужно проверить старый пароль, если юзер есть в базе.
        val personId = request.pwOpt.get.personId
        EmailPwIdent.findByPersonId(personId).flatMap { epws =>
          if (epws.isEmpty) {
            // Юзер меняет пароль, но залогинен через moz persona.
            MozillaPersonaIdent.findByPersonId(personId)
              .map { mps =>
                if (mps.isEmpty) {
                  LOGGER.warn("changePasswordSubmit(): Unknown user session: " + personId)
                  None
                } else {
                  val mp = mps.head
                  val epw = EmailPwIdent(email = mp.email, personId = mp.personId, pwHash = MPersonIdent.mkHash(newPw), isVerified = true)
                  Some(epw)
                }
              }
          } else {
            // Юзер меняет пароль, но у него уже есть EmailPw-логины на s.io.
            val result = epws
              .find { _.checkPassword(oldPw) }
              .map { _.copy(pwHash = MPersonIdent.mkHash(newPw)) }
            Future successful result
          }
        } flatMap {
          case Some(epw) =>
            epw.save
              .flatMap { _ => RdrBackOrFut(r)(changePwOkRdrDflt) }
              .map { _.flashing("success" -> "Новый пароль сохранён.") }
          case None =>
            val formWithErrors = changePasswordFormM.withGlobalError("error.password.invalid")
            onError(formWithErrors)
        }
      }
    )
  }

}


/** Добавить обработчик сабмита формы логина по email и паролю в контроллер. */
trait EmailPwSubmit extends SioController with PlayMacroLogsI with BruteForceProtect {
  import Ident.{EmailPwLoginForm_t, emailPwLoginFormM}

  def emailSubmitOkCall(personId: String)(implicit request: AbstractRequestWithPwOpt[_]): Future[Call] = {
    MarketLk.getMarketRdrCallFor(personId) map { callOpt =>
      callOpt getOrElse routes.Market.index()
    }
  }

  def emailSubmitError(lf: EmailPwLoginForm_t, r: Option[String])(implicit request: AbstractRequestWithPwOpt[_]): Future[Result]

  /** Самбит формы логина по email и паролю. */
  def emailPwLoginFormSubmit(r: Option[String]) = IsAnon.async { implicit request =>
    bruteForceProtected {
      emailPwLoginFormM.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug("emailPwLoginFormSubmit(): Form bind failed:\n" + formatFormErrors(formWithErrors))
          emailSubmitError(formWithErrors, r)
        },
        {case (email1, pw1) =>
          EmailPwIdent.getByEmail(email1) flatMap { epwOpt =>
            if (epwOpt.exists(_.checkPassword(pw1))) {
              // Логин удался.
              // TODO Нужно дать возможность режима сессии "чужой компьютер".
              val personId = epwOpt.get.personId
              RdrBackOrFut(r) { emailSubmitOkCall(personId) }
                .map { _.withSession(username -> personId) }
            } else {
              val lf = emailPwLoginFormM.fill(email1 -> "")
              val lfe = lf.withGlobalError("error.unknown.email_pw")
              emailSubmitError(lfe, r)
            }
          }
        }
      )
    }
  }

}

