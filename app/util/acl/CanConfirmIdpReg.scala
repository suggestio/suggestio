package util.acl

import io.suggest.ym.model.MAdnNode
import models.usr.MExtIdent
import play.api.mvc.{Result, Request, ActionBuilder}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.{PlayMacroLogsI, PlayMacroLogsDyn}
import util.ident.IdentUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.02.15 11:03
 * Description: Юзер, залогинившийся через внешнего провайдера идентификакции, требует
 * подтверждения регистрации (создания первой ноды).
 */
trait CanConfirmIdpRegBase extends ActionBuilder[AbstractRequestWithPwOpt] with PlayMacroLogsI {
  override def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    pwOpt match {
      case Some(pw) =>
        val pcntFut = MAdnNode.countByPersonId(pw.personId)
          .map(_.toInt)
        val hasExtIdent = MExtIdent.countByPersonId(pw.personId)
          .map(_ > 0L)
        val srmFut = SioReqMd.fromPwOpt(pwOpt)
        pcntFut flatMap { pcnt =>
          if (pcnt > 0) {
            LOGGER.debug(s"User[${pw.personId}] already have $pcnt nodes. Refusing reg.confirmation.")
            onAlreadyConfirmed(pw, request)
          } else {
            // Юзер пока не имеет узлов.
            hasExtIdent flatMap {
              case true =>
                srmFut flatMap { srm =>
                  val req1 = RequestWithPwOpt(pwOpt, request, srm)
                  block(req1)
                }

              case false =>
                LOGGER.debug(s"User[${pw.personId}] has no MExtIdents. IdP reg not allowed.")
                onAlreadyConfirmed(pw, request)
            }
          }
        }

      case None =>
        LOGGER.trace("User not logged in.")
        IsAuth.onUnauth(request)
    }
  }

  /** Что возвращать залогиненному юзеру, если для него это действие недопустимо? */
  def onAlreadyConfirmed(pw: PersonWrapper, request: Request[_]): Future[Result] = {
    // Вызвать редиректор, который найдёт для юзера пристанище.
    IdentUtil.redirectUserSomewhere(pw.personId)
  }
}

sealed trait CanConfirmIdpRegBase2
  extends CanConfirmIdpRegBase
  with ExpireSession[AbstractRequestWithPwOpt]
  with PlayMacroLogsDyn

/** Реализация [[CanConfirmIdpRegBase]] без CSRF-действий. */
object CanConfirmIdpReg     extends CanConfirmIdpRegBase2

/** Реализация [[CanConfirmIdpRegBase]] с выставлением CSRF-токена. */
object CanConfirmIdpRegGet  extends CanConfirmIdpRegBase2 with CsrfGet[AbstractRequestWithPwOpt]

/** Реализация [[CanConfirmIdpRegBase]] с проверкой CSRF-токена. */
object CanConfirmIdpRegPost extends CanConfirmIdpRegBase2 with CsrfPost[AbstractRequestWithPwOpt]

