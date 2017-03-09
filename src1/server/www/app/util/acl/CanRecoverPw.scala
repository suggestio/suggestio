package util.acl

import com.google.inject.Inject
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.www.util.acl.SioActionBuilderOuter
import models.mproj.ICommonDi
import models.req.{IReq, MRecoverPwReq, MReq, MUserInit}
import models.usr._
import play.api.mvc._
import util.ident.IdentUtil

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

class CanRecoverPw @Inject() (
                               aclUtil                : AclUtil,
                               identUtil              : IdentUtil,
                               emailPwIdents          : EmailPwIdents,
                               emailActivations       : EmailActivations,
                               mCommonDi              : ICommonDi
                             )
  extends SioActionBuilderOuter
  with MacroLogsImpl
{

  import mCommonDi._


  /** Собрать ACL ActionBuilder проверки доступа на восстановление пароля.
    *
    * @param eActId id активатора.
    * @param keyNotFoundF Не найден ключ для восстановления.
    */
  def apply(eActId: String, userInits1: MUserInit*)
           (keyNotFoundF: IReq[_] => Future[Result]): ActionBuilder[MRecoverPwReq] = {

    new SioActionBuilderImpl[MRecoverPwReq] with InitUserCmds {

      override def userInits = userInits1

      override def invokeBlock[A](request: Request[A], block: (MRecoverPwReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)
        lazy val logPrefix = s"CanRecoverPw($eActId)#${user.personIdOpt.orNull}:"

        val eaOptFut = emailActivations.getById(eActId)

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
                LOGGER.debug(s"$logPrefix ok, epw id = ${epw.idOrNull}")
                runF(eAct, epw)

              // should never occur: Почему-то нет парольной записи для активатора.
              // Такое возможно, если юзер взял ключ инвайта в маркет и вставил его в качестве ключа восстановления пароля.
              case None =>
                LOGGER.error(s"$logPrefix eAct exists, but emailPw is NOT! Hacker? eAct = $eAct")
                keyNotFoundF( _reqErr )
            }


          // Суперюзер (верстальщик например) должен иметь доступ без шаманства.
          case result if user.isSuper =>
            val personId = user.personIdOpt.get
            LOGGER.trace(s"$logPrefix Superuser mocking activation...")
            val epwOptFut = emailPwIdents.findByPersonId(personId)

            maybeInitUser(user)

            val epwFut = epwOptFut
              .map(_.head)
              .recover {
                // should never occur
                case ex: NoSuchElementException =>
                  LOGGER.warn(s"$logPrefix Oops, superuser access for unknown epw! $personId Mocking...", ex)
                  EmailPwIdent("mock@suggest.io", personId = personId, pwHash = "", isVerified = true)
              }
            val ea = result.getOrElse {
              LOGGER.debug(s"$logPrefix Superuser requested form with invalid/inexisting activation: $result")
              EmailActivation("mocked@suggest.io", key = "keykeykeykeykey", id = Some("idididididididid"))
            }

            for {
              epw  <- epwFut
              resp <- runF(ea, epw)
            } yield {
              resp.flashing("error" -> "Using mocked activation.")
            }


          // Остальные случаи -- мимо кассы
          case other =>
            user.personIdOpt.fold {
              // Юзер неизвестен и ключ неизвестен. Возможно, перебор ключей какой-то?
              LOGGER.warn(s"$logPrefix Unknown eAct key: $other")
              keyNotFoundF( _reqErr )
            } { personId =>
              // Вероятно, юзер повторно перешел по ссылке из письма.
              identUtil.redirectUserSomewhere(personId)
            }

        }
      }

    }
  }

}
