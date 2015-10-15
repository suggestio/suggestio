package util.acl

import controllers.IDb
import io.suggest.di.IExecutionContext
import models.req.SioReqMd
import models.{MBillTariffFee, MBillContract}
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
  extends IDb
  with IExecutionContext
  with IsSuperuserUtilCtl
{

  trait IsSuperuserFeeTariffContractBase
    extends ActionBuilder[FeeTariffRequest]
    with IsSuperuserUtil
  {

    /** id запрашиваемого fee-тарифа. */
    def tariffId: Int

    override def invokeBlock[A](request: Request[A], block: (FeeTariffRequest[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      if ( PersonWrapper.isSuperuser(pwOpt) ) {
        val dbFut = Future {
          db.withConnection { implicit c =>
            val _tariff = MBillTariffFee.getById(tariffId).get
            val _contract = MBillContract.getById(_tariff.contractId).get
            _tariff -> _contract
          }
        }(AsyncUtil.jdbcExecutionContext)

        val sioReqMdFut = SioReqMd.fromPwOpt(pwOpt)

        dbFut flatMap { case (tariff, contract) =>
          sioReqMdFut flatMap { srm =>
            val req1 = FeeTariffRequest(tariff, contract, pwOpt, request, srm)
            block(req1)
          }
        }

      } else {
        supOnUnauthFut(request, pwOpt)
      }
    }

  }

  case class IsSuperuserFeeTariffContract(tariffId: Int)
    extends IsSuperuserFeeTariffContractBase
    with ExpireSession[FeeTariffRequest]

}


case class FeeTariffRequest[A](
  tariff      : MBillTariffFee,
  contract    : MBillContract,
  pwOpt       : PwOpt_t,
  request     : Request[A],
  sioReqMd    : SioReqMd
)
  extends AbstractRequestWithPwOpt[A](request)

