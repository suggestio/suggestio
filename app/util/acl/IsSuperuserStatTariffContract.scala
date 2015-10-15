package util.acl

import controllers.IDb
import io.suggest.di.IExecutionContext
import models.{MBillContract, MBillTariffStat}
import models.req.SioReqMd
import util.async.AsyncUtil
import scala.concurrent.Future
import play.api.mvc.{Request, ActionBuilder, Result}
import util.acl.PersonWrapper.PwOpt_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:34
 * Description: Аддон для контроллера с гибридом IsSuperuser + доступ к stat-tariff.
 */
trait IsSuperuserStatTariffContract
  extends IDb
  with IExecutionContext
  with IsSuperuserUtilCtl
{

  trait IsSuperuserStatTariffContractBase
    extends ActionBuilder[StatTariffRequest]
    with IsSuperuserUtil
  {

    /** id запрашиваемого stat-тарифа. */
    def tariffId: Int

    override def invokeBlock[A](request: Request[A], block: (StatTariffRequest[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      if ( PersonWrapper.isSuperuser(pwOpt) ) {
        val dbFut = Future {
          db.withConnection { implicit c =>
            val _tariff = MBillTariffStat.getById(tariffId).get
            val _contract = MBillContract.getById(_tariff.contractId).get
            _tariff -> _contract
          }
        }(AsyncUtil.jdbcExecutionContext)
        val sioReqMdFut = SioReqMd.fromPwOpt(pwOpt)
        dbFut flatMap { case (tariff, contract) =>
          sioReqMdFut flatMap { srm =>
            val req1 = StatTariffRequest(tariff, contract, pwOpt, request, srm)
            block(req1)
          }
        }

      } else {
        onUnauthFut(request, pwOpt)
      }
    }

  }

  case class IsSuperuserStatTariffContract(override val tariffId: Int)
    extends IsSuperuserStatTariffContractBase
    with ExpireSession[StatTariffRequest]

}


case class StatTariffRequest[A](
  tariff    : MBillTariffStat,
  contract  : MBillContract,
  pwOpt     : PwOpt_t,
  request   : Request[A],
  sioReqMd  : SioReqMd
)
  extends AbstractRequestWithPwOpt[A](request)

