package util.acl

import com.google.inject.Inject
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.contract.MContracts
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
                                   val csrf     : Csrf,
                                   mContracts   : MContracts,
                                   mCommonDi    : ICommonDi
                                 ) {

  import mCommonDi._

  sealed trait IsSuNodeContractBase
    extends ActionBuilder[MNodeContractReq]
    with IsSuUtil
  {

    /** id запрашиваемого узла. */
    def nodeId: String

    override def invokeBlock[A](request: Request[A], block: (MNodeContractReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      def reqErr = MReq(request, user)

      if (user.isSuper) {
        val mnodeOptFut = mNodesCache.getById(nodeId)
        mnodeOptFut.flatMap {
          case Some(mnode) =>
            val mcOptFut = FutureUtil.optFut2futOpt( mnode.billing.contractId ) { contractId =>
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

  /** Доступ к узлу с контрактом с выставлением CSRF-токена. */
  case class Get(override val nodeId: String)
    extends IsSuNodeContractAbstract
    with csrf.Get[MNodeContractReq]

  /** Доступ к узлу с контрактом с проверкой CSRF-токена при сабмитах. */
  case class Post(override val nodeId: String)
    extends IsSuNodeContractAbstract
    with csrf.Post[MNodeContractReq]

}

trait IIsSuNodeContract {
  val isSuNodeContract: IsSuNodeContract
}
