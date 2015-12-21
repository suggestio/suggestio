package util.acl

import models.mbill.MContract
import models.req.{MNodeContractReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result}
import util.async.AsyncUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:05
 * Description: Аддон для контроллеров для связки IsSuperuser + [[MContract]].getById()
 * и параллельному чтению узла.
 */
trait IsSuperuserContractNode
  extends IsSuperuserUtilCtl
  with Csrf
{

  import mCommonDi._

  trait IsSuperuserContractNodeBase
    extends ActionBuilder[MNodeContractReq]
    with IsSuperuserUtil
  {

    /** id запрашиваемого контракта. */
    def contractId: Int

    override def invokeBlock[A](request: Request[A], block: (MNodeContractReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      if (user.isSuper) {

        val contractFut = Future {
          db.withConnection { implicit c =>
            MContract.getById(contractId).get
          }
        }(AsyncUtil.jdbcExecutionContext)

        val mnodeOptFut = contractFut.flatMap { mcontract =>
          mNodeCache.getById(mcontract.adnId)
        }

        contractFut flatMap { mcontract =>
          mnodeOptFut flatMap { mnodeOpt =>
            val mnode = mnodeOpt.get
            val req1 = MNodeContractReq(mnode, mcontract, request, user)
            block(req1)
          }
        }

      } else {
        val req1 = MReq(request, user)
        supOnUnauthFut(req1)
      }
    }
  }

  abstract class IsSuperuserContractNodeAbstract
    extends IsSuperuserContractNodeBase
    with ExpireSession[MNodeContractReq]

  case class IsSuperuserContractNode(override val contractId: Int)
    extends IsSuperuserContractNodeAbstract

  /** Реализация [[IsSuperuserContractNodeBase]] с выставлением CSRF-токена. */
  case class IsSuperuserContractNodeGet(override val contractId: Int)
    extends IsSuperuserContractNodeAbstract
    with CsrfGet[MNodeContractReq]

  /** Реализация [[IsSuperuserContractNodeBase]] с проверкой CSRF-токена. */
  case class IsSuperuserContractNodePost(override val contractId: Int)
    extends IsSuperuserContractNodeAbstract
    with CsrfPost[MNodeContractReq]

}
