package util.acl

import javax.inject.Inject
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.node.{MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.common.fut.FutureUtil.HellImplicits.any2fut
import io.suggest.es.model.{EsModel, MEsNestedSearch}
import io.suggest.req.ReqUtil
import models.req.MReq
import play.api.inject.Injector
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}
import util.ident.IdentUtil

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.02.15 11:03
 * Description: Юзер, залогинившийся через внешнего провайдера идентификакции, требует
 * подтверждения регистрации (создания первой ноды).
 */
final class CanConfirmIdpReg @Inject() (
                                         injector                 : Injector,
                                       )
  extends MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val aclUtil = injector.instanceOf[AclUtil]
  private lazy val identUtil = injector.instanceOf[IdentUtil]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val isAuth = injector.instanceOf[IsAuth]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]


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
          import esModel.api._

          // Запустить подсчет имеющихся у юзера магазинов
          val msearch = new MNodeSearch {
            override val outEdges = {
              val cr = Criteria(
                predicates  = MPredicates.OwnedBy :: Nil,
                nodeIds     = personId :: Nil,
              )
              MEsNestedSearch(
                clauses = cr :: Nil,
              )
            }
            override def limit = 5
          }
          val pcntFut = mNodes.dynCount(msearch)
          // Запустить поиск имеющихся внешних идентов
          val hasExtIdentFut = mNodes.dynExists {
            new MNodeSearch {
              override val withIds = personId :: Nil
              override val nodeTypes = MNodeTypes.Person :: Nil
              override val outEdges: MEsNestedSearch[Criteria] = {
                val cr = Criteria(
                  predicates = MPredicates.Ident.Id :: Nil,
                  extService = Some(Nil)
                )
                MEsNestedSearch( cr :: Nil )
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


  def apply(): ActionBuilder[MReq, AnyContent] =
    new ImplC

}
