package util.acl

import play.api.mvc._
import scala.concurrent.Future
import util.Logs
import scala.Some
import play.api.mvc.SimpleResult
import controllers.Application.http404Fut

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.10.13 13:48
 * Description: Суперпользователи сервиса имеют все необходимые права, в т.ч. для доступа в /sys/.
 */
object IsSuperuser extends ActionBuilder[AbstractRequestWithPwOpt] with Logs {
  import LOGGER._
  
  protected def invokeBlock[A](request: Request[A], block: (AbstractRequestWithPwOpt[A]) => Future[SimpleResult]): Future[SimpleResult] = {
    val pwOpt = PersonWrapper.getFromRequest(request)
    pwOpt match {
      case Some(pw) if pw.isSuperuser =>
        trace(s"for user ${pw.id} :: ${request.method} ${request.path}")
        block(new RequestWithPwOpt[A](pwOpt, request))

      case _ => onUnauthFut(request)
    }
  }

  def onUnauthFut(request: RequestHeader): Future[SimpleResult] = {
    import request._
    warn(s"$method $path <- Unauthorized access to hidden/priveleged place from $remoteAddress")
    http404Fut(request)
  }

}
