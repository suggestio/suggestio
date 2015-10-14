package util.acl

import controllers.SioController
import io.suggest.di.IEsClient
import models.req.SioReqMd
import models.usr.{EmailActivation, EmailPwIdent}
import play.api.mvc._
import util.di.IIdentUtil
import views.html.ident.recover._
import scala.concurrent.Future
import util.acl.PersonWrapper.PwOpt_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.15 16:08
 * Description:
 * ActionBuilder для некоторых экшенов восстановления пароля. Завязан на некоторые фунции контроллера, поэтому
 * лежит здесь.
 *
 * Всё сделано в виде аддона для контроллера, т.к. DI-зависимость так проще всего разрулить.
 */

trait CanRecoverPwCtl extends SioController with BruteForceProtectBase with IEsClient with IIdentUtil {

  /** Трейт с базовой логикой action-builder'а CanRecoverPw. */
  trait CanRecoverPwBase extends ActionBuilder[RecoverPwRequest] {

    def eActId: String

    protected def keyNotFound(implicit request: RequestHeader): Future[Result] = {
      RichRequestHeader(request) map { implicit rh =>
        NotFound( failedColTpl() )
      }
    }

    override def invokeBlock[A](request: Request[A], block: (RecoverPwRequest[A]) => Future[Result]): Future[Result] = {
      lazy val logPrefix = s"CanRecoverPw($eActId): "
      bruteForceProtectedNoimpl(request) {
        val pwOpt = PersonWrapper.getFromRequest(request)
        val eaOptFut = EmailActivation.getById(eActId)
        val srmFut = SioReqMd.fromPwOpt(pwOpt)
        def runF(eAct: EmailActivation, epw: EmailPwIdent): Future[Result] = {
          srmFut flatMap { srm =>
            val req1 = RecoverPwRequest(request, pwOpt, epw, eAct, srm)
            block(req1)
          }
        }
        eaOptFut.flatMap {
          // Юзер обращается по корректной активационной записи.
          case Some(eAct) =>
            EmailPwIdent.getById(eAct.email) flatMap {
              case Some(epw) if epw.personId == eAct.key =>
                // Можно отрендерить блок
                LOGGER.debug(logPrefix + "ok: " + request.path)
                runF(eAct, epw)

              // should never occur: Почему-то нет парольной записи для активатора.
              // Такое возможно, если юзер взял ключ инвайта в маркет и вставил его в качестве ключа восстановления пароля.
              case None =>
                LOGGER.error(s"${logPrefix}eAct exists, but emailPw is NOT! Hacker? pwOpt = $pwOpt ;; eAct = $eAct")
                keyNotFound(request)
            }

          // Суперюзер (верстальщик например) должен иметь доступ без шаманства.
          case result if PersonWrapper.isSuperuser(pwOpt) =>
            LOGGER.trace("Superuser mocking activation...")
            val epwFut = EmailPwIdent.findByPersonId(pwOpt.get.personId)
              .map(_.head)
              .recover {
                // should never occur
                case ex: NoSuchElementException =>
                  LOGGER.warn("Oops, superuser access for unknown epw! " + pwOpt + " Mocking...")
                  EmailPwIdent("mock@suggest.io", personId = pwOpt.get.personId, pwHash = "", isVerified = true)
              }
            val ea = result getOrElse {
              LOGGER.debug("Superuser requested form with invalid/inexisting activation: " + result)
              EmailActivation("mocked@suggest.io", key = "keykeykeykeykey", id = Some("idididididididid"))
            }
            epwFut flatMap { epw =>
              runF(ea, epw)
                .map { _.flashing("error" -> "Using mocked activation.") }
            }

          // Остальные случаи -- мимо кассы
          case _ =>
            pwOpt match {
              // Вероятно, юзер повторно перешел по ссылке из письма.
              case Some(pw) =>
                identUtil.redirectUserSomewhere(pw.personId)
              // Юзер неизвестен и ключ неизвестен. Возможно, перебор ключей какой-то?
              case None =>
                LOGGER.warn(logPrefix + "Unknown eAct key. pwOpt = " + pwOpt)
                keyNotFound(request)
            }
        }
      }
    }
  }

  /** Реализация [[CanRecoverPwBase]] с выставлением CSRF-токена. */
  case class CanRecoverPwGet(eActId: String)
    extends CanRecoverPwBase
    with CsrfGet[RecoverPwRequest]
    with ExpireSession[RecoverPwRequest]

  /** Реализация [[CanRecoverPwBase]] с проверкой CSRF-токена. */
  case class CanRecoverPwPost(eActId: String)
    extends CanRecoverPwBase
    with CsrfPost[RecoverPwRequest]
    with ExpireSession[RecoverPwRequest]

}


/** Реквест активации. */
case class RecoverPwRequest[A](
  request   : Request[A],
  pwOpt     : PwOpt_t,
  epw       : EmailPwIdent,
  eAct      : EmailActivation,
  sioReqMd  : SioReqMd
)
  extends AbstractRequestWithPwOpt(request)


