package util.acl

import play.api.mvc._
import scala.concurrent.Future
import util.{PlayMacroLogsImpl, Logs}
import scala.Some
import play.api.mvc.SimpleResult
import controllers.Application.http404Fut
import util.acl.PersonWrapper.PwOpt_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.13 13:48
 * Description: Суперпользователи сервиса имеют все необходимые права, в т.ч. для доступа в /sys/.
 */
object IsSuperuser extends ActionBuilder[AbstractRequestWithPwOpt] with PlayMacroLogsImpl {
  import LOGGER._
  
  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    pwOpt match {
      case Some(pw) if pw.isSuperuser =>
        trace(s"for user ${pw.id} :: ${request.method} ${request.path}")
        block(new RequestWithPwOpt[A](pwOpt, request))

      case _ => onUnauthFut(request, pwOpt)
    }
  }

  def onUnauthFut(request: RequestHeader, pwOpt: PwOpt_t): Future[SimpleResult] = {
    import request._
    warn(s"$method $path <- BLOCKED access to hidden/priveleged place from $remoteAddress user=$pwOpt")
    http404Fut(request)
  }

}
