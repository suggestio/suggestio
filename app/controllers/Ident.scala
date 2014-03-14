package controllers

import play.api.Play.current
import play.api.libs.ws._
import play.api.data._
import play.api.data.Forms._
import util.acl._
import util._
import play.api.mvc._
import views.html.ident._
import play.api.libs.concurrent.Promise.timeout
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import scala.concurrent.Future
import play.api.libs.json.JsString
import models._
import play.api.mvc.Security.username
import play.api.i18n.Lang
import SiowebEsUtil.client
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 11:47
 * Description: Контроллер обычного логина в систему. Обычно логинятся через Mozilla Persona, но не исключено, что
 * в будущем будет также и вход по имени/паролю для некоторых учетных записей.
 */

object Ident extends SioController with Logs {

  import LOGGER._

  // URL, используемый для person'a. Если сие запущено на локалхосте, то надо менять этот адресок.
  val AUDIENCE_URL = current.configuration.getString("persona.audience.url").get
  val VERIFIER_URL = current.configuration.getString("persona.verify.url") getOrElse "https://verifier.login.persona.org/verify"

  val personaM = Form(
    "assertion" -> nonEmptyText(minLength = 5)
  )

  val verifyReqTimeout = 10.seconds
  protected val verifyReqTimeoutMs = verifyReqTimeout.toMillis.toInt
  protected val verifyReqFutureTimeout = verifyReqTimeout + 200.milliseconds

  /** Начало логина через Mozilla Persona. Нужно отрендерить страницу со скриптами. */
  def persona = MaybeAuth { implicit request =>
    request.pwOpt match {
      // Уже залогинен -- отправить в админку
      case Some(_) => rdrToAdmin
      case None    => Ok(personaTpl())
    }
  }

  type EmailPwLoginForm_t = Form[(String, String)]

  /** Форма логина по email и паролю. */
  val emailPwLoginFormM: EmailPwLoginForm_t = Form(tuple(
    "email"    -> email,
    "password" -> FormUtil.passwordM
  ))


  /**
   * Юзер завершает логин через persona. Нужно тут принять значения audience, проверить, залогинить юзера
   * и отправить в админку
   * @return
   */
  def persona_submit = Action.async { implicit request =>
    lazy val logPrefix = s"personaSubmit() ${request.remoteAddress}: "
    trace(logPrefix + "starting...")
    personaM.bindFromRequest.fold(
      {formWithErrors =>
        warn(logPrefix + "Cannot parse POST body: " + formWithErrors.errors)
        Future.successful(BadRequest)
      }
      ,
      {assertion =>
        trace(logPrefix + "mozilla persona assertion received in POST: " + assertion)
        val reqBody : Map[String, Seq[String]] = Map(
          "assertion" -> Seq(assertion),
          "audience"  -> Seq(AUDIENCE_URL)
        )
        val futureVerify = WS
          .url(VERIFIER_URL)
          .withRequestTimeout(verifyReqTimeoutMs)
          .post(reqBody)
        // На время запроса неопределенной длительности необходимо освободить текущий поток, поэтому возвращаем фьючерс:
        val timeoutFuture = timeout("timeout", verifyReqFutureTimeout)
        Future.firstCompletedOf(Seq(futureVerify, timeoutFuture)) flatMap {
          // Получен ответ от сервера mozilla persona.
          case resp: WSResponse =>
            val respJson = resp.json
            trace(logPrefix + s"MP verifier resp: ${resp.status} ${resp.statusText} :: " + respJson)
            respJson \ "status" match {
              // Всё ок. Нужно награбить email и залогинить/зарегать юзера
              case JsString("okay") =>
                // В доках написано, что нужно сверять AudienceURL, присланный сервером персоны
                // TODO переписать этот многоэтажный АД
                respJson \ "audience" match {
                  case JsString(AUDIENCE_URL) =>
                    // Запускаем юзера в студию
                    val email = (respJson \ "email").as[String].trim
                    trace(logPrefix + "found email: " + email)
                    // Найти текущего юзера или создать нового:
                    MozillaPersonaIdent.getById(email) flatMap { mpIdOpt =>
                      val personFut: Future[MPerson] = mpIdOpt match {
                        case None =>
                          trace(logPrefix + "Registering new user: " + email)
                          val mperson = new MPerson(lang=lang.code)
                          mperson.save.flatMap { personId =>
                            MozillaPersonaIdent(email=email, personId=personId).save.map { _ =>
                              mperson.id = Some(personId)
                              mperson
                            }
                          }

                        case Some(mpId) =>
                          trace(logPrefix + "Login already registered user with ident: " + mpId)
                          // Восстановить язык из сохранненого добра
                          mpId.person.flatMap {
                            case Some(p) => Future successful p
                            // Невероятный сценарий: из MPerson слетела запись.
                            case None => recreatePersonIdFor(mpId)
                          }
                      }
                      personFut flatMap { person =>
                        // Заапрувить анонимно-добавленные и подтвержденные домены (qi)
                        DomainQi.installFromSession(person.personId) map { session1 =>
                          // Делаем info чтобы в логах мониторить логины пользователей.
                          trace(logPrefix + "successfully logged in as " + email)
                          // залогинить юзера наконец, выставив язык, сохранённый ранее в состоянии.
                          rdrToAdmin
                            .withLang(Lang(person.lang))
                            .withSession(session1)
                            .withSession(username -> person.personId)
                        }
                      }
                    }

                  // Юзер подменил audience url, значит его assertion невалиден. Либо мы запустили на локалхосте продакшен.
                  case other =>
                    // TODO использовать логгирование, а не сие:
                    warn("Invalid audience URL: " + other)
                    Forbidden("Broken URL in credentials. Please try again.")
                } // проверка audience

              // Юзер что-то мухлюет или persona глючит
              case JsString("failure") =>
                warn("invalid credentials")
                NotAcceptable("Mozilla Persona: invalid credentials")

              // WTF
              case other =>
                val msg = "Mozilla Persona: unsupported response format."
                error(msg + "status = " + other + "\n\n" + respJson)
                InternalServerError(msg)
            } // проверка status

          case "timeout" =>
            val msg = "Mozilla Persona server does not responding."
            warn(msg)
            InternalServerError(msg)
        } // тело фьючерса ws-запроса
      } // матчинг assertion из присланных пользователем данных
    )
  }


  /**
   * Юзер разлогинивается. Выпилить из сессии данные о его логине.
   * @return Редирект на главную, ибо анонимусу идти больше некуда.
   */
  def logout = Action { implicit request =>
    Redirect(routes.Application.index())
      .withSession(session - username)
  }


  /** Рендер страницы с возможностью логина по email и паролю. */
  def emailPwLoginForm = MaybeAuth { implicit request =>
    Ok(emailPwLoginFormTpl(emailPwLoginFormM))
  }

  /** Самбит формы логина по email и паролю. */
  def emailPwLoginFormSubmit = MaybeAuth.async { implicit request =>
    emailPwLoginFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug("emailPwLoginFormSubmit(): Form bind failed " + formWithErrors.errors)
        NotAcceptable(emailPwLoginFormTpl(formWithErrors))
      },
      {case (email1, pw1) =>
        EmailPwIdent.getByEmail(email1) map { epwOpt =>
          if (epwOpt.exists(_.checkPassword(pw1))) {
            // логин удался. TODO Надо редиректить куда-то в sio-маркет.
            Redirect(routes.Admin.index())
              .withSession(username -> epwOpt.get.personId)
          } else {
            val lf = emailPwLoginFormM.fill(email1 -> "")
            val lfe = lf.withGlobalError("invalid.login.credentials")
            Forbidden(emailPwLoginFormTpl(lfe))
          }
        }
      }
    )
  }

  /** Функция обхода foreign-key ошибок */
  private def recreatePersonIdFor(mpi: MPersonIdent[_])(implicit request: RequestHeader): Future[MPerson] = {
    val logPrefix = s"recreatePersonIdFor($mpi): "
    error(logPrefix + s"MPerson not found for ident $mpi. Suppressing internal error: creating new one...")
    val p = MPerson(id=Some(mpi.personId), lang=lang.code)
    p.save.map {
      case _personId if _personId == mpi.personId =>
        warn(logPrefix + s"Emergency recreated MPerson(${_personId}) for identity $mpi")
        p
      case _personId =>
        // [Невозможный сценарий] Не удаётся пересоздать запись MPerson с корректным id.
        error(s"Unable to recreate MPerson record for ident $mpi. oldPersonId=${mpi.personId} != newPersonId=${_personId}")
        // rollback создания записи.
        MPerson.deleteById(_personId) onComplete {
          case Success(isDeleted) => warn(logPrefix + s"Deleted recreated $p for zombie ident $mpi")
          case Failure(ex) => error("Failed to rollback. Storage failure!", ex)
        }
        p
    }
  }

  // TODO выставить нормальный routing тут
  protected def rdrToAdmin = Redirect(routes.Application.index())

}

