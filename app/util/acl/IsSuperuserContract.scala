package util.acl

import models.mbill
import models.mbill.MContract
import models.req.{MContract1Req, MReq}
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
    extends ActionBuilder[MContract1Req]
    with IsSuperuserUtil
  {

    /** id запрашиваемого контракта. */
    def contractId: Int

    override def invokeBlock[A](request: Request[A], block: (MContract1Req[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      if (user.isSuper) {

        val contractFut = Future {
          db.withConnection { implicit c =>
            MContract.getById(contractId).get
          }
        }(AsyncUtil.jdbcExecutionContext)

        contractFut.flatMap { mcontract =>
          val req1 = MContract1Req(mcontract, request, user)
          block(req1)
        }

      } else {
        val req1 = MReq(request, user)
        supOnUnauthFut(req1)
      }
    }
  }

  case class IsSuperuserContract(contractId: Int)
    extends IsSuperuserContractBase
    with ExpireSession[MContract1Req]

}
