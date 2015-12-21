package util.acl

import models.mbill.{MContract, MTariffStat}
import models.req.{MTariffStatContractReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result}
import util.async.AsyncUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:34
 * Description: Аддон для контроллера с гибридом IsSuperuser + доступ к stat-tariff.
 */
trait IsSuperuserStatTariffContract
  extends IsSuperuserUtilCtl
{

  import mCommonDi._

  trait IsSuperuserStatTariffContractBase
    extends ActionBuilder[MTariffStatContractReq]
    with IsSuperuserUtil
  {

    /** id запрашиваемого stat-тарифа. */
    def tariffId: Int

    override def invokeBlock[A](request: Request[A], block: (MTariffStatContractReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      if (user.isSuper) {

        val dbFut = Future {
          db.withConnection { implicit c =>
            val _tariff = MTariffStat.getById(tariffId).get
            val _contract = MContract.getById(_tariff.contractId).get
            _tariff -> _contract
          }
        }(AsyncUtil.jdbcExecutionContext)

        dbFut flatMap { case (tariff, contract) =>
          val req1 = MTariffStatContractReq(tariff, contract, request, user)
          block(req1)
        }

      } else {
        val req1 = MReq(request, user)
        supOnUnauthFut(req1)
      }
    }

  }

  case class IsSuperuserStatTariffContract(override val tariffId: Int)
    extends IsSuperuserStatTariffContractBase
    with ExpireSession[MTariffStatContractReq]

}
