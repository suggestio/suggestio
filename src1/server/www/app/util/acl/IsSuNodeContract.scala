package util.acl

import com.google.inject.Inject
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.contract.MContracts
import io.suggest.www.util.acl.SioActionBuilderOuter
import models.mproj.ICommonDi
import models.req.{IReqHdr, MNodeContractReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.15 11:59
 * Description: Доступ к узлу и его контракту для суперюзера.
 */
class IsSuNodeContract @Inject() (
                                   aclUtil      : AclUtil,
                                   mContracts   : MContracts,
                                   isSu         : IsSu,
                                   mCommonDi    : ICommonDi
                                 )
  extends SioActionBuilderOuter
{

  import mCommonDi._


  /** Доступ к узлу с контрактом.
    * @param nodeId id запрашиваемого узла.
    */
  def apply(nodeId: String): ActionBuilder[MNodeContractReq] = {
    new SioActionBuilderImpl[MNodeContractReq] {

      override def invokeBlock[A](request: Request[A], block: (MNodeContractReq[A]) => Future[Result]): Future[Result] = {
        val user = aclUtil.userFromRequest(request)

        def reqErr = MReq(request, user)

        if (user.isSuper) {
          val mnodeOptFut = mNodesCache.getById(nodeId)
          mnodeOptFut.flatMap {
            case Some(mnode) =>
              val mcOptFut = FutureUtil.optFut2futOpt(mnode.billing.contractId) { contractId =>
                val act = mContracts.getById(contractId)
                slick.db.run(act)
              }

              mcOptFut.flatMap {
                case Some(mc) =>
                  val req1 = MNodeContractReq(mnode, mc, request, user)
                  block(req1)

                case None =>
                  contractNotFound(reqErr)
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

      def contractNotFound(req: IReqHdr): Future[Result] = {
        errorHandler.http404Fut(req)
      }

    }
  }

}

trait IIsSuNodeContract {
  val isSuNodeContract: IsSuNodeContract
}
