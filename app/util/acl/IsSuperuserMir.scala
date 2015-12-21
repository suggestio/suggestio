package util.acl

import controllers.SioController
import models.MInviteRequest
import models.req.{MirReq, MReq}
import play.api.mvc.{ActionBuilder, Request, Result}
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
  with IsSuperuserUtilCtl
  with IInviteRequest
{

  import mCommonDi._

  trait IsSuperuserMirBase
    extends ActionBuilder[MirReq]
    with IsSuperuserUtil
  {

    def mirId: String

    def isMirStateOk(mir: MInviteRequest) = true

    override def invokeBlock[A](request: Request[A], block: (MirReq[A]) => Future[Result]): Future[Result] = {
      val mirOptFut = mInviteRequest.getById(mirId)

      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)

      if (user.isSuper) {
        mirOptFut.flatMap {
          case Some(mir) =>
            if (isMirStateOk(mir)) {
              val req1 = MirReq(mir, request, user)
              block(req1)
            } else {
              mirStateInvalid
            }

          case None =>
            mirNotFound
        }

      } else {
        val req1 = MReq(request, user)
        supOnUnauthFut(req1)
      }
    }

    def mirStateInvalidMsg = s"MIR[$mirId] has impossible state for this action."

    def mirNotFound: Future[Result] = {
      NotFound("Invite request not found: " + mirId)
    }

    def mirStateInvalid: Future[Result] = {
      ExpectationFailed(mirStateInvalidMsg)
    }

  }


  case class IsSuperuserMir(mirId: String)
    extends IsSuperuserMirBase
    with ExpireSession[MirReq]

}
