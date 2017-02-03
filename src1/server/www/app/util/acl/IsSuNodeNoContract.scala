package util.acl

import controllers.SioController
import models.MNode
import models.req.{IReqHdr, MReq, MNodeReq}
import play.api.mvc.{Result, Request, ActionBuilder}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.15 12:15
 * Description: Доступ суперюзера к узлу, обязательно БЕЗ контракта.
 */
trait IsSuNodeNoContract
  extends SioController
  with IsSuperuserUtilCtl
  with Csrf
{

  import mCommonDi._

  sealed trait IsSuNodeNoContractBase
    extends ActionBuilder[MNodeReq]
    with IsSuperuserUtil
  {

    /** id запрашиваемого узла. */
    def nodeId: String

    override def invokeBlock[A](request: Request[A], block: (MNodeReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      def reqErr = MReq(request, user)

      if (user.isSuper) {
        val mnodeOptFut = mNodesCache.getById(nodeId)
        mnodeOptFut flatMap {
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
        supOnUnauthFut(reqErr)
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

  /** Доступ суперюзера к узлу без контракта. */
  case class IsSuNodeNoContract(override val nodeId: String)
    extends IsSuNodeNoContractAbstract

  /** Доступ суперюзера к узлу без контракта с выставлением CSRF-токена. */
  case class IsSuNodeNoContractGet(override val nodeId: String)
    extends IsSuNodeNoContractAbstract
    with CsrfGet[MNodeReq]

  /** Доступ суперюзера к узлу без контракта с проверкой CSRF-токена при сабмитах. */
  case class IsSuNodeNoContractPost(override val nodeId: String)
    extends IsSuNodeNoContractAbstract
    with CsrfPost[MNodeReq]

}
