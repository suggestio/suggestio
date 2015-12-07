package util.acl

import controllers.SioController
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import models.MNode
import models.req.SioReqMd
import models.usr.MExtIdent
import play.api.mvc.{Result, Request, ActionBuilder}
import util.di.IIdentUtil
import util.{PlayMacroLogsI, PlayMacroLogsDyn}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.02.15 11:03
 * Description: Юзер, залогинившийся через внешнего провайдера идентификакции, требует
 * подтверждения регистрации (создания первой ноды).
 */
trait CanConfirmIdpReg
  extends SioController
  with IIdentUtil
  with OnUnauthUtilCtl
  with Csrf
{

  import mCommonDi._

  /** Код базовой реализации ActionBuilder'ов, проверяющих возможность подтверждения регистрации. */
  trait CanConfirmIdpRegBase
    extends ActionBuilder[AbstractRequestWithPwOpt]
    with PlayMacroLogsI
    with OnUnauthUtil
  {
    override def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      pwOpt match {
        case Some(pw) =>
          // Разрешить суперюзеру доступ, чтобы можно было верстать и проверять форму без шаманств.
          val hasAccess: Future[Boolean] = if (PersonWrapper isSuperuser pwOpt) {
            Future successful true
          } else {
            // Запустить подсчет имеющихся у юзера магазинов
            val msearch = new MNodeSearchDfltImpl {
              override def outEdges = {
                val cr = Criteria(Seq(pw.personId), Seq(MPredicates.OwnedBy))
                Seq(cr)
              }
              override def limit    = 5
            }
            val pcntFut = MNode.dynCount(msearch)
            // Запустить поиск имеющихся внешних идентов
            val hasExtIdent = MExtIdent.countByPersonId(pw.personId)
              .map(_ > 0L)
            // Дождаться результата поиска узлов.
            pcntFut flatMap { pcnt =>
              if (pcnt > 0L) {
                LOGGER.debug(s"User[${pw.personId}] already have $pcnt or more nodes. Refusing reg.confirmation.")
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
              onAlreadyConfirmed(pw.personId, request)
          }

        case None =>
          LOGGER.trace("User not logged in.")
          onUnauth(request)
      }
    }

    /** Что возвращать залогиненному юзеру, если для него это действие недопустимо? */
    def onAlreadyConfirmed(personId: String, request: Request[_]): Future[Result] = {
      // Вызвать редиректор, который найдёт для юзера пристанище.
      identUtil.redirectUserSomewhere(personId)
    }
  }

  sealed abstract class CanConfirmIdpRegBase2
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
