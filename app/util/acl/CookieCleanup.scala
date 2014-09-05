package util.acl

import play.api.mvc.{Result, Request, ActionBuilder}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.09.14 15:45
 * Description: Вычищать всякие _boss_session и прочие куки, которые уже не нужны.
 */
trait CookieCleanup [R[_]] extends ActionBuilder[R] {
  override def invokeBlock[A](request: Request[A], block: (R[A]) => Future[Result]): Future[Result] = {
    ???
  }
}
