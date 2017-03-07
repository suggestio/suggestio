package util.acl

import com.google.inject.Inject
import io.suggest.www.util.acl.SioActionBuilderOuter
import models.MNode
import models.mproj.ICommonDi
import models.req.{IReqHdr, MNodeReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.15 12:15
 * Description: Доступ суперюзера к узлу, обязательно БЕЗ контракта.
 */
class IsSuNodeNoContract @Inject() (
                                     aclUtil    : AclUtil,
                                     isSu       : IsSu,
                                     mCommonDi  : ICommonDi
                                   )
  extends SioActionBuilderOuter
{

  import mCommonDi._

  /**
    * @param nodeId id запрашиваемого узла.
    */
  def apply(nodeId: String): ActionBuilder[MNodeReq] = {
    new SioActionBuilderImpl[MNodeReq] {

      override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        def reqErr = MReq(request, user)

        if (user.isSuper) {
          val mnodeOptFut = mNodesCache.getById(nodeId)
          mnodeOptFut.flatMap {
            case Some(mnode) =>
              if (mnode.billing.contractId.isEmpty) {
                val req1 = MNodeReq(mnode, request, user)
                block(req1)
              } else {
                nodeHasContract(mnode, reqErr)
              }

            case None =>
              nodeNotFound(reqErr)
          }

        } else {
          isSu.supOnUnauthFut(reqErr)
        }
      }

      def nodeNotFound(req: IReqHdr): Future[Result] = {
        errorHandler.http404Fut(req)
      }

      def nodeHasContract(mnode: MNode, req: IReqHdr): Future[Result] = {
        nodeNotFound(req)
      }

    }
  }

}

