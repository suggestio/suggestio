package util.acl

import play.api.mvc._
import scala.concurrent.Future
import util.PlayMacroLogsImpl
import scala.Some
import play.api.mvc.Result
import controllers.Application.http404Fut
import util.acl.PersonWrapper.PwOpt_t
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.13 13:48
 * Description: Суперпользователи сервиса имеют все необходимые права, в т.ч. для доступа в /sys/.
 */
object IsSuperuser extends ActionBuilder[AbstractRequestWithPwOpt] with PlayMacroLogsImpl {
  import LOGGER._
  
  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[Result]): Future[Result] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    val sioReqMdFut = SioReqMd.fromPwOpt(pwOpt)
    pwOpt match {
      case Some(pw) if pw.isSuperuser =>
        trace(s"for user ${pw.personId} :: ${request.method} ${request.path}")
        sioReqMdFut flatMap { srm =>
          block(RequestWithPwOpt(pwOpt, request, srm))
        }

      case _ => onUnauthFut(request, pwOpt)
    }
  }

  def onUnauthFut(request: RequestHeader, pwOpt: PwOpt_t): Future[Result] = {
    import request._
    warn(s"$method $path <- BLOCKED access to hidden/priveleged place from $remoteAddress user=$pwOpt")
    http404Fut(request)
  }

}
