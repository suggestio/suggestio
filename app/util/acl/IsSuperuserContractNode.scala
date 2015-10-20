package util.acl

import controllers.IDb
import io.suggest.di.{IExecutionContext, IEsClient}
import models.{MAdnNode, MBillContract}
import models.req.SioReqMd
import util.async.AsyncUtil
import util.di.INodeCache
import scala.concurrent.Future
import play.api.mvc.{ActionBuilder, Request, Result}
import util.acl.PersonWrapper.PwOpt_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:05
 * Description: Аддон для контроллеров для связки IsSuperuser + [[models.MBillContract]].getById()
 * и параллельному чтению узла.
 */
trait IsSuperuserContractNode
  extends IDb
  with IEsClient
  with IExecutionContext
  with IsSuperuserUtilCtl
  with INodeCache
{

  trait IsSuperuserContractNodeBase
    extends ActionBuilder[ContractNodeRequest]
    with IsSuperuserUtil
  {

    /** id запрашиваемого контракта. */
    def contractId: Int

    override def invokeBlock[A](request: Request[A], block: (ContractNodeRequest[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      if ( PersonWrapper.isSuperuser(pwOpt) ) {
        val sioReqMdFut = SioReqMd.fromPwOpt(pwOpt)
        val contractFut = Future {
          db.withConnection { implicit c =>
            MBillContract.getById(contractId).get
          }
        }(AsyncUtil.jdbcExecutionContext)
        contractFut flatMap { contract =>
          mNodeCache.getById(contract.adnId) flatMap { adnNodeOpt =>
            val adnNode = adnNodeOpt.get
            sioReqMdFut flatMap { srm =>
              val req1 = ContractNodeRequest(contract, adnNode, pwOpt, request, srm)
              block(req1)
            }
          }
        }

      } else {
        supOnUnauthFut(request, pwOpt)
      }
    }
  }

  abstract class IsSuperuserContractNodeAbstract
    extends IsSuperuserContractNodeBase
    with ExpireSession[ContractNodeRequest]

  case class IsSuperuserContractNode(override val contractId: Int)
    extends IsSuperuserContractNodeAbstract

  /** Реализация [[IsSuperuserContractNodeBase]] с выставлением CSRF-токена. */
  case class IsSuperuserContractNodeGet(override val contractId: Int)
    extends IsSuperuserContractNodeAbstract
    with CsrfGet[ContractNodeRequest]

  /** Реализация [[IsSuperuserContractNodeBase]] с проверкой CSRF-токена. */
  case class IsSuperuserContractNodePost(override val contractId: Int)
    extends IsSuperuserContractNodeAbstract
    with CsrfPost[ContractNodeRequest]

}


case class ContractNodeRequest[A](
  contract  : MBillContract,
  adnNode   : MAdnNode,
  pwOpt     : PwOpt_t,
  request   : Request[A],
  sioReqMd  : SioReqMd
)
  extends AbstractContractRequest(request)

