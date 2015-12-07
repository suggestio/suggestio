package util.acl

import models.req.SioReqMd
import scala.concurrent.Future
import util.PlayMacroLogsDyn
import play.api.mvc.{RequestHeader, Request, ActionBuilder, Result}
import util.acl.PersonWrapper.PwOpt_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.13 13:48
 * Description: Суперпользователи сервиса имеют все необходимые права, в т.ч. для доступа в /sys/.
 */
trait IsSuperuserUtilCtl extends OnUnauthUtilCtl {

  trait IsSuperuserUtil extends OnUnauthUtil with PlayMacroLogsDyn {

    def supOnUnauthFut(request: RequestHeader, pwOpt: PwOpt_t): Future[Result] = {
      import request._
      LOGGER.warn(s"$method $path <- BLOCKED access to hidden/priveleged place from $remoteAddress user=$pwOpt")
      supOnUnauthResult(request, pwOpt)
    }

    def supOnUnauthResult(request: RequestHeader, pwOpt: PwOpt_t): Future[Result] = {
      onUnauth(request)
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
    extends ActionBuilder[AbstractRequestWithPwOpt]
    with IsSuperuserUtil
  {

    protected def isAllowed(pwOpt: PwOpt_t): Boolean = {
      PersonWrapper.isSuperuser(pwOpt)
    }

    override def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[Result]): Future[Result] = {
      val pwOpt = PersonWrapper.getFromRequest(request)
      if (isAllowed(pwOpt)) {
        val sioReqMdFut = SioReqMd.fromPwOpt(pwOpt)
        LOGGER.trace(s"for user $pwOpt :: ${request.method} ${request.path}")
        sioReqMdFut flatMap { srm =>
          block( RequestWithPwOpt(pwOpt, request, srm) )
        }
      } else {
        supOnUnauthFut(request, pwOpt)
      }
    }

  }


  sealed abstract class IsSuperuserAbstract
    extends IsSuperuserBase
    with ExpireSession[AbstractRequestWithPwOpt]
    with CookieCleanup[AbstractRequestWithPwOpt]

  object IsSuperuser
    extends IsSuperuserAbstract

  object IsSuperuserGet
    extends IsSuperuserAbstract
    with CsrfGet[AbstractRequestWithPwOpt]

  object IsSuperuserPost
    extends IsSuperuserAbstract
    with CsrfPost[AbstractRequestWithPwOpt]

}

