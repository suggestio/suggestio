package util.acl

import controllers.SioController
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.IMNodes
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import models.req.MReq
import models.usr.MExtIdent
import play.api.mvc.{ActionBuilder, Request, Result}
import util.di.IIdentUtil
import util.{PlayMacroLogsDyn, PlayMacroLogsI}

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
  with IMNodes
{

  import mCommonDi._

  /** Код базовой реализации ActionBuilder'ов, проверяющих возможность подтверждения регистрации. */
  trait CanConfirmIdpRegBase
    extends ActionBuilder[MReq]
    with PlayMacroLogsI
    with OnUnauthUtil
  {
    override def invokeBlock[A](request: Request[A], block: (MReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)

      personIdOpt.fold {
        LOGGER.trace("User not logged in.")
        onUnauth(request)

      } { personId =>
        val user = mSioUsers(personIdOpt)
        // Разрешить суперюзеру доступ, чтобы можно было верстать и проверять форму без шаманств.
        val hasAccess: Future[Boolean] = if (user.isSuper) {
          Future successful true
        } else {
          // Запустить подсчет имеющихся у юзера магазинов
          val msearch = new MNodeSearchDfltImpl {
            override def outEdges = {
              val cr = Criteria(Seq(personId), Seq(MPredicates.OwnedBy))
              Seq(cr)
            }
            override def limit = 5
          }
          val pcntFut = mNodes.dynCount(msearch)
          // Запустить поиск имеющихся внешних идентов
          val hasExtIdent = MExtIdent.countByPersonId(personId)
            .map(_ > 0L)
          // Дождаться результата поиска узлов.
          pcntFut flatMap { pcnt =>
            if (pcnt > 0L) {
              LOGGER.debug(s"User[$personId] already have $pcnt or more nodes. Refusing reg.confirmation.")
              Future successful false
            } else {
              // Юзер пока не имеет узлов. Проверить наличие идентов.
              hasExtIdent.filter(identity).onFailure {
                case ex: NoSuchElementException =>
                  LOGGER.debug(s"User[$personId] has no MExtIdents. IdP reg not allowed.")
              }
              hasExtIdent
            }
          }
        }
        hasAccess flatMap {
          case true =>
            val req1 = MReq(request, user)
            block(req1)

          case false =>
            onAlreadyConfirmed(personId, request)
        }
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
    with ExpireSession[MReq]
    with PlayMacroLogsDyn

  /** Реализация [[CanConfirmIdpRegBase]] с выставлением CSRF-токена. */
  object CanConfirmIdpRegGet
    extends CanConfirmIdpRegBase2
    with CsrfGet[MReq]

  /** Реализация [[CanConfirmIdpRegBase]] с проверкой CSRF-токена. */
  object CanConfirmIdpRegPost
    extends CanConfirmIdpRegBase2
    with CsrfPost[MReq]

}
