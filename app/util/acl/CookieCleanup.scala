package util.acl

import play.api.mvc.{Result, Request, ActionBuilder}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.09.14 15:45
 * Description: Вычищать всякие _boss_session и прочие куки, которые уже не нужны.
 */

object CookieCleanup {

  val BAD_NAMES = Set("_boss_session")

}


import CookieCleanup._


trait CookieCleanup [R[_]] extends ActionBuilder[R] {
  abstract override def invokeBlock[A](request: Request[A], block: (R[A]) => Future[Result]): Future[Result] = {
    val superFut = super.invokeBlock(request, block)
    ???
  }
}
