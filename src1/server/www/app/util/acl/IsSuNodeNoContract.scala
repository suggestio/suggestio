package util.acl

import com.google.inject.Inject
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
                                     val csrf   : Csrf,
                                     isSu       : IsSu,
                                     mCommonDi  : ICommonDi
                                   ) {

  import mCommonDi._

  sealed trait IsSuNodeNoContractBase
    extends ActionBuilder[MNodeReq]
  {

    /** id запрашиваемого узла. */
    def nodeId: String

    override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      def reqErr = MReq(request, user)

      if (user.isSuper) {
        val mnodeOptFut = mNodesCache.getById(nodeId)
        mnodeOptFut.flatMap {
          case Some(mnode) =>
            if ( mnode.billing.contractId.isEmpty ) {
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


  sealed abstract class IsSuNodeNoContractAbstract
    extends IsSuNodeNoContractBase
    with ExpireSession[MNodeReq]

  /** Доступ суперюзера к узлу без контракта с выставлением CSRF-токена. */
  case class Get(override val nodeId: String)
    extends IsSuNodeNoContractAbstract
    with csrf.Get[MNodeReq]

  /** Доступ суперюзера к узлу без контракта с проверкой CSRF-токена при сабмитах. */
  case class Post(override val nodeId: String)
    extends IsSuNodeNoContractAbstract
    with csrf.Post[MNodeReq]

}

