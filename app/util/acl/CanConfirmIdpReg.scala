package util.acl

import controllers.{IEsClient, SioController}
import models.MAdnNode
import models.req.SioReqMd
import models.usr.MExtIdent
import play.api.mvc.{Result, Request, ActionBuilder}
import util.{PlayMacroLogsI, PlayMacroLogsDyn}
import util.ident.IIdentUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.02.15 11:03
 * Description: Юзер, залогинившийся через внешнего провайдера идентификакции, требует
 * подтверждения регистрации (создания первой ноды).
 */
trait CanConfirmIdpRegCtl extends SioController with IEsClient with IIdentUtil {

  /** Код базовой реализации ActionBuilder'ов, проверяющих возможность подтверждения регистрации. */
  trait CanConfirmIdpRegBase extends ActionBuilder[AbstractRequestWithPwOpt] with PlayMacroLogsI {
    override def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      pwOpt match {
        case Some(pw) =>
          // Разрешить суперюзеру доступ, чтобы можно было верстать и проверять форму без шаманств.
          val hasAccess: Future[Boolean] = if (PersonWrapper isSuperuser pwOpt) {
            Future successful true
          } else {
            val pcntFut = MAdnNode.countByPersonId(pw.personId)
              .map(_.toInt)
            val hasExtIdent = MExtIdent.countByPersonId(pw.personId)
              .map(_ > 0L)
            pcntFut flatMap { pcnt =>
              if (pcnt > 0) {
                LOGGER.debug(s"User[${pw.personId}] already have $pcnt nodes. Refusing reg.confirmation.")
                Future successful false
              } else {
                // Юзер пока не имеет узлов. Проверить наличие идентов.
                hasExtIdent.filter(identity).onFailure {
                  case ex: NoSuchElementException =>
                    LOGGER.debug(s"User[${pw.personId}] has no MExtIdents. IdP reg not allowed.")
                }
                hasExtIdent
              }
            }
          }
          val srmFut = SioReqMd.fromPwOpt(pwOpt)
          hasAccess flatMap {
            case true =>
              srmFut flatMap { srm =>
                val req1 = RequestWithPwOpt(pwOpt, request, srm)
                block(req1)
              }

            case false =>
              onAlreadyConfirmed(pw, request)
          }

        case None =>
          LOGGER.trace("User not logged in.")
          IsAuth.onUnauth(request)
      }
    }

    /** Что возвращать залогиненному юзеру, если для него это действие недопустимо? */
    def onAlreadyConfirmed(pw: PersonWrapper, request: Request[_]): Future[Result] = {
      // Вызвать редиректор, который найдёт для юзера пристанище.
      identUtil.redirectUserSomewhere(pw.personId)
    }
  }

  sealed trait CanConfirmIdpRegBase2
    extends CanConfirmIdpRegBase
    with ExpireSession[AbstractRequestWithPwOpt]
    with PlayMacroLogsDyn

  /** Реализация [[CanConfirmIdpRegBase]] с выставлением CSRF-токена. */
  object CanConfirmIdpRegGet
    extends CanConfirmIdpRegBase2
    with CsrfGet[AbstractRequestWithPwOpt]

  /** Реализация [[CanConfirmIdpRegBase]] с проверкой CSRF-токена. */
  object CanConfirmIdpRegPost
    extends CanConfirmIdpRegBase2
    with CsrfPost[AbstractRequestWithPwOpt]

}
