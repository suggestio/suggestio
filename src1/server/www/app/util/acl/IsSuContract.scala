package util.acl

import controllers.SioController
import io.suggest.mbill2.m.contract.IMContracts
import io.suggest.mbill2.m.gid.Gid_t
import models.req.{IReq, MReq, MContractReq}
import play.api.mvc.{Result, Request, ActionBuilder}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.16 15:03
  * Description: Доступ админа к контракту биллинга-2 по id контракта.
  */
trait IsSuContract
  extends SioController
  with Csrf
  with IMContracts
{

  import mCommonDi._

  sealed trait IsSuContractBase
    extends ActionBuilder[MContractReq]
    with IsSuperuserUtil
  {

    /** id запрошенного контракта. */
    def contractId: Gid_t

    override def invokeBlock[A](request: Request[A], block: (MContractReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      def reqErr = MReq(request, user)

      if (user.isSuper) {
        val mcOptFut = slick.db.run {
          mContracts.getById(contractId)
        }

        mcOptFut.flatMap {
          case Some(mc) =>
            val req1 = MContractReq(mc, request, user)
            block(req1)

          case None =>
            contractNotFound(reqErr)
        }

      } else {
        supOnUnauthFut(reqErr)
      }
    }

    def contractNotFound(req: IReq[_]): Future[Result] = {
      errorHandler.http404Fut(req)
    }

  }


  sealed abstract class IsSuContractAbstract
    extends IsSuContractBase
    with ExpireSession[MContractReq]

  case class IsSuContract(override val contractId: Gid_t)
    extends IsSuContractAbstract

  case class IsSuContractGet(override val contractId: Gid_t)
    extends IsSuContractAbstract
    with CsrfGet[MContractReq]

  case class IsSuContractPost(override val contractId: Gid_t)
    extends IsSuContractAbstract
    with CsrfPost[MContractReq]

}
