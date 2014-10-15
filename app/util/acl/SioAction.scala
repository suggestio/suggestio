package util.acl

import play.api.mvc.{Result, Request, ActionBuilder}
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.10.14 9:38
 * Description: Когда нужен голый экшен с доступом к Request через SioWrappedRequest, используется этот билдер.
 */
object SioAction extends ActionBuilder[SioWrappedRequest] {
  override def invokeBlock[A](request: Request[A], block: (SioWrappedRequest[A]) => Future[Result]): Future[Result] = {
    block(request)
  }
}
