package util.acl

import play.api.http.HeaderNames
import play.filters.csrf.{CSRFCheck, CSRFAddToken}
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.02.15 17:56
 * Description: Поддержка CSRF-защиты в контроллерах.
 */


object CsrfUtil {

  def withNoCache(result: Result): Result = {
    result.withHeaders(
      HeaderNames.VARY          -> "Set-Cookie,Cookie",
      // Cache-Control режет токен, если НЕ пустой либо НЕ содержит no-cache.
      HeaderNames.CACHE_CONTROL -> "private, no-cache, must-revalidate"
    )
  }

}


import CsrfUtil._


/** Аддон для action-builder'ов, добавляющий выставление CSRF-токена в сессию. */
trait CsrfGet[R[_]] extends ActionBuilder[R] {

  abstract override def invokeBlock[A](request: Request[A], block: (R[A]) => Future[Result]): Future[Result] = {
    super.invokeBlock(request, block)
      .map { withNoCache }
  }

  override protected def composeAction[A](action: Action[A]): Action[A] = {
    CSRFAddToken( super.composeAction(action) )
  }
}


/** Аддон для action-builder'ов, добавляющий проверку CSRF-токена перед запуском экшена на исполнение. */
trait CsrfPost[R[_]] extends ActionBuilder[R] {

  abstract override def invokeBlock[A](request: Request[A], block: (R[A]) => Future[Result]): Future[Result] = {
    super.invokeBlock(request, block)
      .map { withNoCache }
  }

  override protected def composeAction[A](action: Action[A]): Action[A] = {
    CSRFCheck( super.composeAction(action) )
  }
}

