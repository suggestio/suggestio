package util.acl

import com.google.inject.{Inject, Singleton}
import io.suggest.www.util.acl.SioActionBuilderOuter
import models.mproj.ICommonDi
import models.req.{IReqHdr, MNodeReq, MReq}

import scala.concurrent.Future
import play.api.mvc.{ActionBuilder, Request, Result}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:48
 * Description: Аддон для контроллеров с гибридом IsSuperuser + MAdnNode.getById().
 *
 * 2015.dec.23: Изначально назывался IsSuperuserAdnNode и обслуживал только MAdnNode.
 */
@Singleton
class IsSuNode @Inject() (
                           aclUtil    : AclUtil,
                           isSu       : IsSu,
                           mCommonDi  : ICommonDi
                         )
  extends SioActionBuilderOuter
{

  import mCommonDi._


  /** Часто нужно админить узлы рекламной сети. Тут комбинация IsSuperuser + IsAdnAdmin.
    * @param nodeId id запрашиваемого узла.
    */
  def apply(nodeId: String): ActionBuilder[MNodeReq] = {
    new SioActionBuilderImpl[MNodeReq] {

      override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        if (user.isSuper) {
          val mnodeOptFut = mNodesCache.getById(nodeId)
          mnodeOptFut.flatMap {
            case Some(mnode) =>
              val req1 = MNodeReq(mnode, request, user)
              block(req1)

            case None =>
              val req1 = MReq(request, user)
              nodeNotFound(req1)
          }

        } else {
          val req1 = MReq(request, user)
          isSu.supOnUnauthFut(req1)
        }
      }

    }
  }


  def nodeNotFound(req: IReqHdr): Future[Result] = {
    errorHandler.http404Fut(req)
  }

}

trait IIsSuNodeDi {
  val isSuNode: IsSuNode
}
