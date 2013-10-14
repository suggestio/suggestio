package controllers

import play.api.Play.current
import play.api.libs.ws.{Response, WS}
import play.api.data._
import play.api.data.Forms._
import util.acl._
import util._
import play.api.mvc.{SimpleResult, Action, Controller}
import views.html.ident._
import play.api.libs.concurrent.Promise.timeout
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{Future, future}
import play.api.libs.json.JsString
import models.MPerson
import play.api.mvc.Security.username

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.04.13 11:47
 * Description: Контроллер обычного логина в систему. Обычно логинятся через Mozilla Persona, но не исключено, что
 * в будущем будет также и вход по имени/паролю для некоторых учетных записей.
 */

object Ident extends SioController with Logs {

  // URL, используемый для person'a. Если сие запущено на локалхосте, то надо менять этот адресок.
  val AUDIENCE_URL = current.configuration.getString("persona.audience.url").get
  val VERIFIER_URL = current.configuration.getString("persona.verify.url") getOrElse "https://verifier.login.persona.org/verify"

  val personaM = Form(
    "assertion" -> nonEmptyText(minLength = 5)
  )

  val verifyReqTimeout = 10.seconds
  protected val verifyReqTimeoutMs = verifyReqTimeout.toMillis.toInt
  protected val verifyReqFutureTimeout = verifyReqTimeout + 200.milliseconds

  /**
   * Начало логина через Mozilla Persona. Нужно отрендерить страницу со скриптами.
   */
  def persona = MaybeAuth { implicit request =>
    request.pwOpt match {
      // Уже залогинен -- отправить в админку
      case Some(_) => rdrToAdmin
      case None    => Ok(personaTpl())
    }
  }

  /**
   * Юзер завершает логин через persona. Нужно тут принять значения audience, проверить, залогинить юзера
   * и отправить в админку
   * @return
   */
  def persona_submit = Action.async { implicit request =>
    personaM.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest)
      ,
      {assertion =>
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
          case resp:Response =>
            val respJson = resp.json
            respJson \ "status" match {
              // Всё ок. Нужно награбить email и залогинить/зарегать юзера
              case JsString("okay") =>
                // В доках написано, что нужно сверять AudienceURL, присланный сервером персоны
                // TODO переписать этот многоэтажный АД
                respJson \ "audience" match {
                  case JsString(AUDIENCE_URL) =>
                    // Запускаем юзера в студию
                    val email = (respJson \ "email").as[String].trim
                    // Найти текущего юзера или создать нового:
                    MPerson.getById(email) flatMap { personOpt =>
                      val personFut = personOpt match {
                        case None    => new MPerson(email).save
                        case Some(p) => Future successful p
                      }
                      personFut flatMap { person =>
                        // Заапрувить анонимно-добавленные и подтвержденные домены (qi)
                        DomainQi.installFromSession(person.id) map { session1 =>
                          // залогинить юзера наконец.
                          rdrToAdmin
                            .withSession(session1)
                            .withSession(username -> person.id)
                        }
                      }
                    }

                  // Юзер подменил audience url, значит его assertion невалиден. Либо мы запустили на локалхосте продакшен.
                  case other =>
                    // TODO использовать логгирование, а не сие:
                    LOGGER.warn("Invalid audience URL: " + other)
                    Forbidden("Broken URL in credentials. Please try again.")
                } // проверка audience

              // Юзер что-то мухлюет или persona глючит
              case JsString("failure") =>
                LOGGER.warn("invalid credentials")
                NotAcceptable("Mozilla Persona: invalid credentials")

              // WTF
              case other =>
                val msg = "Mozilla Persona: unsupported response format."
                LOGGER.error(msg + "status = " + other + "\n\n" + respJson)
                InternalServerError(msg)
            } // проверка status

          case "timeout" =>
            val msg = "Mozilla Persona server does not responding."
            LOGGER.warn(msg)
            InternalServerError(msg)
        } // тело фьючерса ws-запроса
      } // матчинг assertion из присланных пользователем данных
    )
  }


  /**
   * Юзер разлогинивается
   * @return
   */
  def logout = Action { implicit request =>
    Redirect(routes.Application.index()).withNewSession
  }


  // TODO выставить нормальный routing тут
  protected def rdrToAdmin = Redirect(routes.Application.index)

}
