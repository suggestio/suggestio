package util.acl

import models.mbill
import models.mbill.MContract
import models.req.{MContractReq, SioReq}
import play.api.mvc.{ActionBuilder, Request, Result}
import util.async.AsyncUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 15:16
 * Description: Аддон для контроллеров с гибридом IsSuperuser + [[mbill.MContract.getById()]].
 */
trait IsSuperuserContract
  extends IsSuperuserUtilCtl
{

  import mCommonDi._

  sealed trait IsSuperuserContractBase
    extends ActionBuilder[MContractReq]
    with IsSuperuserUtil
  {

    /** id запрашиваемого контракта. */
    def contractId: Int

    override def invokeBlock[A](request: Request[A], block: (MContractReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      if (user.isSuperUser) {

        val contractFut = Future {
          db.withConnection { implicit c =>
            MContract.getById(contractId).get
          }
        }(AsyncUtil.jdbcExecutionContext)

        contractFut.flatMap { mcontract =>
          val req1 = MContractReq(mcontract, request, user)
          block(req1)
        }

      } else {
        val req1 = SioReq(request, user)
        supOnUnauthFut(req1)
      }
    }
  }

  case class IsSuperuserContract(contractId: Int)
    extends IsSuperuserContractBase
    with ExpireSession[MContractReq]

}
