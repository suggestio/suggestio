package util.acl

import controllers.IDb
import io.suggest.di.IExecutionContext
import models.MBillContract
import models.req.SioReqMd
import scala.concurrent.Future
import play.api.mvc.{Request, ActionBuilder, Result}
import util.acl.PersonWrapper.PwOpt_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:16
 * Description: Аддон для контроллеров с гибридом IsSuperuser + [[models.MBillContract.getById()]].
 */
trait IsSuperuserContract
  extends IDb
  with IExecutionContext
  with IsSuperuserUtilCtl
{

  sealed trait IsSuperuserContractBase
    extends ActionBuilder[ContractRequest]
    with IsSuperuserUtil
  {

    def contractId: Int

    override def invokeBlock[A](request: Request[A], block: (ContractRequest[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      if (PersonWrapper.isSuperuser(pwOpt)) {
        val sioReqMdFut = SioReqMd.fromPwOpt(pwOpt)
        val contract = db.withConnection { implicit c =>
          MBillContract.getById(contractId).get
        }
        sioReqMdFut flatMap { srm =>
          val req1 = ContractRequest(contract, pwOpt, request, srm)
          block(req1)
        }
      } else {
        onUnauthFut(request, pwOpt)
      }
    }
  }

  case class IsSuperuserContract(contractId: Int)
    extends IsSuperuserContractBase
    with ExpireSession[ContractRequest]

}


abstract class AbstractContractRequest[A](request: Request[A])
  extends AbstractRequestWithPwOpt(request) {
  def contract: MBillContract
}

case class ContractRequest[A](
  contract  : MBillContract,
  pwOpt     : PwOpt_t,
  request   : Request[A],
  sioReqMd  : SioReqMd
)
  extends AbstractContractRequest[A](request)

