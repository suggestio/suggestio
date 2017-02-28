package util.acl

import com.google.inject.Inject
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.MNodes
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.util.logs.{IMacroLogs, MacroLogsDyn}
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut
import io.suggest.sec.util.Csrf
import models.mproj.ICommonDi
import models.req.MReq
import models.usr.MExtIdents
import play.api.mvc.{ActionBuilder, Request, Result}
import util.ident.IdentUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.02.15 11:03
 * Description: Юзер, залогинившийся через внешнего провайдера идентификакции, требует
 * подтверждения регистрации (создания первой ноды).
 */
class CanConfirmIdpReg @Inject() (
                                   identUtil                : IdentUtil,
                                   mNodes                   : MNodes,
                                   mExtIdents               : MExtIdents,
                                   isAuth                   : IsAuth,
                                   val csrf                 : Csrf,
                                   mCommonDi                : ICommonDi
                                 ) {

  import mCommonDi._

  /** Код базовой реализации ActionBuilder'ов, проверяющих возможность подтверждения регистрации. */
  sealed trait CanConfirmIdpRegBase
    extends ActionBuilder[MReq]
    with IMacroLogs
  {
    override def invokeBlock[A](request: Request[A], block: (MReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)

      personIdOpt.fold {
        LOGGER.trace("User not logged in.")
        isAuth.onUnauth(request)

      } { personId =>
        val user = mSioUsers(personIdOpt)
        // Разрешить суперюзеру доступ, чтобы можно было верстать и проверять форму без шаманств.
        val hasAccess: Future[Boolean] = if (user.isSuper) {
          true

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
          val hasExtIdent = mExtIdents.countByPersonId(personId)
            .map(_ > 0L)
          // Дождаться результата поиска узлов.
          pcntFut.flatMap { pcnt =>
            if (pcnt > 0L) {
              LOGGER.debug(s"User[$personId] already have $pcnt or more nodes. Refusing reg.confirmation.")
              false
            } else {
              // Юзер пока не имеет узлов. Проверить наличие идентов.
              hasExtIdent.filter(identity).onFailure {
                case _: NoSuchElementException =>
                  LOGGER.debug(s"User[$personId] has no MExtIdents. IdP reg not allowed.")
              }
              hasExtIdent
            }
          }
        }
        hasAccess.flatMap {
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
    with MacroLogsDyn

  /** Реализация [[CanConfirmIdpRegBase]] с выставлением CSRF-токена. */
  object Get
    extends CanConfirmIdpRegBase2
    with csrf.Get[MReq]

  /** Реализация [[CanConfirmIdpRegBase]] с проверкой CSRF-токена. */
  object Post
    extends CanConfirmIdpRegBase2
    with csrf.Post[MReq]

}
