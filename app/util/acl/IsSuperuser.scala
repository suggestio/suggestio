package util.acl

import models.req.{IReqHdr, ISioUser, MReq}
import scala.concurrent.Future
import util.PlayMacroLogsDyn
import play.api.mvc.{Request, ActionBuilder, Result}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.13 13:48
 * Description: Суперпользователи сервиса имеют все необходимые права, в т.ч. для доступа в /sys/.
 */
trait IsSuperuserUtilCtl extends OnUnauthUtilCtl {

  trait IsSuperuserUtil extends OnUnauthUtil with PlayMacroLogsDyn {

    def supOnUnauthFut(req: IReqHdr): Future[Result] = {
      import req._
      LOGGER.warn(s"$method $path <- BLOCKED access to hidden/priveleged place from $remoteAddress user=${req.user.personIdOpt}")
      supOnUnauthResult(req)
    }

    def supOnUnauthResult(req: IReqHdr): Future[Result] = {
      onUnauth(req)
    }

  }

}


trait IsSuperuser
  extends IsSuperuserUtilCtl
  with CookieCleanupSupport
  with Csrf
{

  import mCommonDi._

  trait IsSuperuserBase
    extends ActionBuilder[MReq]
    with IsSuperuserUtil
  {

    protected def isAllowed(user: ISioUser): Boolean = {
      user.isSuper
    }

    override def invokeBlock[A](request: Request[A], block: (MReq[A]) => Future[Result]): Future[Result] = {
      val personIdOpt = sessionUtil.getPersonId(request)
      val user = mSioUsers(personIdOpt)
      val req1 = MReq(request, user)
      if ( isAllowed(user) ) {
        LOGGER.trace(s"for user $personIdOpt :: ${request.method} ${request.path}")
        block(req1)

      } else {
        supOnUnauthFut(req1)
      }
    }

  }


  sealed abstract class IsSuperuserAbstract
    extends IsSuperuserBase
    with ExpireSession[MReq]
    with CookieCleanup[MReq]

  object IsSuperuser
    extends IsSuperuserAbstract

  object IsSuperuserGet
    extends IsSuperuserAbstract
    with CsrfGet[MReq]

  object IsSuperuserPost
    extends IsSuperuserAbstract
    with CsrfPost[MReq]

}

