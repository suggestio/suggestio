package util.acl

import io.suggest.es.model.EsModel
import io.suggest.n2.node.MNodes

import javax.inject.Inject
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import models.req.{IReq, MNodeReq, MReq}
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.{ActionBuilder, AnyContent, Request, Result}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:48
 * Description: Аддон для контроллеров с гибридом IsSuperuser + MAdnNode.getById().
 *
 * 2015.dec.23: Изначально назывался IsSuperuserAdnNode и обслуживал только MAdnNode.
 */
final class IsSuNode @Inject() (
                                 injector   : Injector,
                                 aclUtil    : AclUtil,
                                 reqUtil    : ReqUtil,
                               )
  extends MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val isSu = injector.instanceOf[IsSu]
  private lazy val errorHandler = injector.instanceOf[HttpErrorHandler]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]


  /** Часто нужно админить узлы рекламной сети. Тут комбинация IsSuperuser + IsAdnAdmin.
    * @param nodeId id запрашиваемого узла.
    */
  def apply(nodeId: String): ActionBuilder[MNodeReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MNodeReq] {

      override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        def req1 = MReq(request, user)

        if (user.isSuper) {
          import esModel.api._

          val mnodeOptFut = mNodes.getByIdCache(nodeId)
          mnodeOptFut.flatMap {
            case Some(mnode) =>
              val req1 = MNodeReq(mnode, request, user)
              block(req1)

            case None =>
              LOGGER.debug(s"Node $nodeId not found")
              errorHandler.onClientError(req1, Status.NOT_FOUND)
          }

        } else {
          isSu.supOnUnauthFut(req1)
        }
      }

    }
  }


  /** Надо, чтобы узел ОТСУТСТВОВАЛ. */
  def nodeMissing(nodeId: String): ActionBuilder[IReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[IReq] {
      override def invokeBlock[A](request: Request[A], block: IReq[A] => Future[Result]): Future[Result] = {
        val req1 = aclUtil.reqFromRequest(request)

        if (req1.user.isSuper) {
          import esModel.api._

          val mnodeOptFut = mNodes.getByIdCache(nodeId)
          mnodeOptFut.flatMap {
            case None =>
              block(req1)
            case _ =>
              LOGGER.debug(s"Node $nodeId exist, but must miss.")
              errorHandler.onClientError(req1, Status.NOT_FOUND)
          }

        } else {
          isSu.supOnUnauthFut(req1)
        }
      }
    }
  }

}
