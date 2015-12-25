package util.acl

import controllers.SioController
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.contract.IMContracts
import models.req.{IReqHdr, MReq, MNodeContractReq}
import play.api.mvc.{Request, Result, ActionBuilder}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.15 11:59
 * Description: Доступ к узлу и его контракту для суперюзера.
 */
trait IsSuNodeContract
  extends SioController
  with IsSuperuserUtilCtl
  with Csrf
  with IMContracts
{

  import mCommonDi._

  sealed trait IsSuNodeContractBase
    extends ActionBuilder[MNodeContractReq]
    with IsSuperuserUtil
  {

    /** id запрашиваемого узла. */
    def nodeId: String

    override def invokeBlock[A](request: Request[A], block: (MNodeContractReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      def reqErr = MReq(request, user)

      if (user.isSuper) {
        val mnodeOptFut = mNodeCache.getById(nodeId)
        mnodeOptFut flatMap {
          case Some(mnode) =>
            val mcOptFut = FutureUtil.optFut2futOpt( mnode.billing.contractId ) { contractId =>
              val act = mContracts.getById(contractId)
              dbConfig.db.run(act)
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
        supOnUnauthFut(reqErr)
      }
    }

    def nodeNotFound(req: IReqHdr): Future[Result] = {
      errorHandler.http404Fut(req)
    }

    def contractNotFound(req: IReqHdr): Future[Result] = {
      errorHandler.http404Fut(req)
    }

  }

  sealed abstract class IsSuNodeContractAbstract
    extends IsSuNodeContractBase
    with ExpireSession[MNodeContractReq]

  /** Доступ к узлу с контрактом. */
  case class IsSuNodeContract(override val nodeId: String)
    extends IsSuNodeContractAbstract

  /** Доступ к узлу с контрактом с выставлением CSRF-токена. */
  case class IsSuNodeContractGet(override val nodeId: String)
    extends IsSuNodeContractAbstract
    with CsrfGet[MNodeContractReq]

  /** Доступ к узлу с контрактом с проверкой CSRF-токена при сабмитах. */
  case class IsSuNodeContractPost(override val nodeId: String)
    extends IsSuNodeContractAbstract
    with CsrfPost[MNodeContractReq]

}
