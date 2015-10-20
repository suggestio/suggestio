package util.acl

import controllers.SioController
import io.suggest.di.{IEsClient, IExecutionContext}
import models.MInviteRequest
import models.req.SioReqMd
import play.api.mvc.{ActionBuilder, Result, Request}
import util.acl.PersonWrapper.PwOpt_t
import util.di.IInviteRequest

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.15 14:56
 * Description:
 */
trait IsSuperuserMir
  extends SioController
  with IExecutionContext
  with IEsClient
  with IsSuperuserUtilCtl
  with IInviteRequest
{

  trait IsSuperuserMirBase
    extends ActionBuilder[MirRequest]
    with IsSuperuserUtil
  {

    def mirId: String

    def isMirStateOk(mir: MInviteRequest) = true

    override def invokeBlock[A](request: Request[A], block: (MirRequest[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      if (PersonWrapper.isSuperuser(pwOpt)) {
        mInviteRequest.getById(mirId) flatMap {
          case Some(mir) =>
            if (isMirStateOk(mir)) {
              SioReqMd.fromPwOpt(pwOpt) flatMap { srm =>
                val req1 = MirRequest(mir, pwOpt, request, srm)
                block(req1)
              }
            } else {
              Future successful mirStateInvalid
            }

          case None =>
            Future successful mirNotFound(mirId)
        }
      } else {
        supOnUnauthFut(request, pwOpt)
      }
    }

    def mirStateInvalidMsg = s"MIR[$mirId] has impossible state for this action."

    def mirNotFound(mirId: String): Result = {
      NotFound("Invite request not found: " + mirId)
    }

    def mirStateInvalid: Result = {
      ExpectationFailed(mirStateInvalidMsg)
    }

  }


  case class IsSuperuserMir(mirId: String)
    extends IsSuperuserMirBase
    with ExpireSession[MirRequest]

}


case class MirRequest[A](
  mir       : MInviteRequest,
  pwOpt     : PwOpt_t,
  request   : Request[A],
  sioReqMd  : SioReqMd
)
  extends AbstractRequestWithPwOpt(request)

