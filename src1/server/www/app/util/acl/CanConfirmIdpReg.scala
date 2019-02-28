package util.acl

import javax.inject.Inject
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.{MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut
import io.suggest.es.model.EsModel
import io.suggest.req.ReqUtil
import models.mproj.ICommonDi
import models.req.MReq
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}
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
                                   esModel                  : EsModel,
                                   aclUtil                  : AclUtil,
                                   identUtil                : IdentUtil,
                                   mNodes                   : MNodes,
                                   isAuth                   : IsAuth,
                                   reqUtil                  : ReqUtil,
                                   mCommonDi                : ICommonDi,
                                 )
  extends MacroLogsImpl
{

  import mCommonDi._
  import esModel.api._


  /** Код базовой реализации ActionBuilder'ов, проверяющих возможность подтверждения регистрации. */
  private class ImplC extends reqUtil.SioActionBuilderImpl[MReq] {

    override def invokeBlock[A](request: Request[A], block: (MReq[A]) => Future[Result]): Future[Result] = {
      val user = aclUtil.userFromRequest(request)

      user.personIdOpt.fold {
        LOGGER.trace("User not logged in.")
        isAuth.onUnauth(request)

      } { personId =>
        // Разрешить суперюзеру доступ, чтобы можно было верстать и проверять форму без шаманств.
        val hasAccess: Future[Boolean] = if (user.isSuper) {
          true

        } else {
          // Запустить подсчет имеющихся у юзера магазинов
          val msearch = new MNodeSearchDfltImpl {
            override def outEdges = {
              val cr = Criteria(
                predicates  = MPredicates.OwnedBy :: Nil,
                nodeIds     = personId :: Nil,
              )
              cr :: Nil
            }
            override def limit = 5
          }
          val pcntFut = mNodes.dynCount(msearch)
          // Запустить поиск имеющихся внешних идентов
          val hasExtIdentFut = mNodes.dynExists {
            new MNodeSearchDfltImpl {
              override val withIds = personId :: Nil
              override val nodeTypes = MNodeTypes.Person :: Nil
              override val outEdges: Seq[Criteria] = {
                val cr = Criteria(
                  predicates = MPredicates.Ident.Id :: Nil,
                  extService = Some(Nil)
                )
                cr :: Nil
              }
              override def limit = 1
            }
          }
          // Дождаться результата поиска узлов.
          pcntFut.flatMap { pcnt =>
            if (pcnt > 0L) {
              LOGGER.debug(s"User[$personId] already have $pcnt or more nodes. Refusing reg.confirmation.")
              false
            } else {
              // Юзер пока не имеет узлов. Проверить наличие идентов.
              for {
                hasExtIdent <- hasExtIdentFut
                if !hasExtIdent
              } {
                LOGGER.debug(s"User[$personId] has no MExtIdents. IdP reg not allowed.")
              }
              hasExtIdentFut
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


  final def apply(): ActionBuilder[MReq, AnyContent] =
    new ImplC

}
