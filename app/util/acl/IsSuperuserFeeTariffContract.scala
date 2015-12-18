package util.acl

import models.mbill.{MTariffFee, MContract}
import models.req.{SioReq, MTariffFeeContractReq, SioReqMd}
import play.api.mvc.{Result, ActionBuilder, Request}
import util.acl.PersonWrapper.PwOpt_t
import util.async.AsyncUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:40
 * Description:
 */
trait IsSuperuserFeeTariffContract
  extends IsSuperuserUtilCtl
{

  import mCommonDi._

  trait IsSuperuserFeeTariffContractBase
    extends ActionBuilder[MTariffFeeContractReq]
    with IsSuperuserUtil
  {

    /** id запрашиваемого fee-тарифа. */
    def tariffId: Int

    override def invokeBlock[A](request: Request[A], block: (MTariffFeeContractReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      if (user.isSuperUser) {

        val dbFut = Future {
          db.withConnection { implicit c =>
            val _tariff = MTariffFee.getById(tariffId).get
            val _contract = MContract.getById(_tariff.contractId).get
            _tariff -> _contract
          }
        }(AsyncUtil.jdbcExecutionContext)

        dbFut flatMap { case (tariff, contract) =>
          val req1 = MTariffFeeContractReq(tariff, contract, request, user)
          block(req1)
        }

      } else {
        val req1 = SioReq(request, user)
        supOnUnauthFut(req1)
      }
    }

  }

  case class IsSuperuserFeeTariffContract(tariffId: Int)
    extends IsSuperuserFeeTariffContractBase
    with ExpireSession[MTariffFeeContractReq]

}
