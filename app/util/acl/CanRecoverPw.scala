package util.acl

import controllers.SioController
import models.req.{IReq, MRecoverPwReq, MReq, MUserInit}
import models.usr.{EmailActivation, EmailPwIdent, IEmailPwIdentsDi}
import play.api.mvc._
import util.di.IIdentUtil
import views.html.ident.recover._

import scala.concurrent.Future

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

trait CanRecoverPw
  extends SioController
  with BruteForceProtectBase
  with IIdentUtil
  with Csrf
  with IEmailPwIdentsDi
{

  import mCommonDi._

  /** Трейт с базовой логикой action-builder'а CanRecoverPw. */
  trait CanRecoverPwBase extends ActionBuilder[MRecoverPwReq] with InitUserCmds {

    /** id активатора. */
    def eActId: String

    protected def keyNotFound(implicit req: IReq[_]): Future[Result] = {
      NotFound( failedColTpl() )
    }

    override def invokeBlock[A](request: Request[A], block: (MRecoverPwReq[A]) => Future[Result]): Future[Result] = {
      lazy val logPrefix = s"CanRecoverPw($eActId): "

      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      val wrappedReq = MReq(request, user)

      bruteForceProtectedNoimpl(wrappedReq) {
        val eaOptFut = EmailActivation.getById(eActId)
        def runF(eAct: EmailActivation, epw: EmailPwIdent): Future[Result] = {
          val req1 = MRecoverPwReq(epw, eAct, request, user)
          block(req1)
        }
        def _reqErr = MReq(request, user)
        eaOptFut.flatMap {
          // Юзер обращается по корректной активационной записи.
          case Some(eAct) =>
            val epwIdentFut = emailPwIdents.getById(eAct.email)
            maybeInitUser(user)
            epwIdentFut.flatMap {
              case Some(epw) if epw.personId == eAct.key =>
                // Можно отрендерить блок
                LOGGER.debug(logPrefix + "ok: " + request.path)
                runF(eAct, epw)

              // should never occur: Почему-то нет парольной записи для активатора.
              // Такое возможно, если юзер взял ключ инвайта в маркет и вставил его в качестве ключа восстановления пароля.
              case None =>
                LOGGER.error(s"${logPrefix}eAct exists, but emailPw is NOT! Hacker? pwOpt = $personIdOpt ;; eAct = $eAct")
                keyNotFound(_reqErr)
            }

          // Суперюзер (верстальщик например) должен иметь доступ без шаманства.
          case result if user.isSuper =>
            val personId = personIdOpt.get
            LOGGER.trace("Superuser mocking activation...")
            val epwOptFut = emailPwIdents.findByPersonId(personId)
            maybeInitUser(user)
            val epwFut = epwOptFut
              .map(_.head)
              .recover {
                // should never occur
                case ex: NoSuchElementException =>
                  LOGGER.warn("Oops, superuser access for unknown epw! " + personId + " Mocking...")
                  EmailPwIdent("mock@suggest.io", personId = personId, pwHash = "", isVerified = true)
              }
            val ea = result getOrElse {
              LOGGER.debug("Superuser requested form with invalid/inexisting activation: " + result)
              EmailActivation("mocked@suggest.io", key = "keykeykeykeykey", id = Some("idididididididid"))
            }
            epwFut.flatMap { epw =>
              runF(ea, epw)
                .map { _.flashing("error" -> "Using mocked activation.") }
            }

          // Остальные случаи -- мимо кассы
          case _ =>
            personIdOpt match {
              // Вероятно, юзер повторно перешел по ссылке из письма.
              case Some(personId) =>
                identUtil.redirectUserSomewhere(personId)
              // Юзер неизвестен и ключ неизвестен. Возможно, перебор ключей какой-то?
              case None =>
                LOGGER.warn(logPrefix + "Unknown eAct key. pwOpt = " + personIdOpt)
                keyNotFound(_reqErr)
            }
        }
      }
    }
  }

  /** Реализация [[CanRecoverPwBase]] с выставлением CSRF-токена. */
  case class CanRecoverPwGet(override val eActId: String, override val userInits: MUserInit*)
    extends CanRecoverPwBase
    with CsrfGet[MRecoverPwReq]
    with ExpireSession[MRecoverPwReq]

  /** Реализация [[CanRecoverPwBase]] с проверкой CSRF-токена. */
  case class CanRecoverPwPost(override val eActId: String, override val userInits: MUserInit*)
    extends CanRecoverPwBase
    with CsrfPost[MRecoverPwReq]
    with ExpireSession[MRecoverPwReq]

}
