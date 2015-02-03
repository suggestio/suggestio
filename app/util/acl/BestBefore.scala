package util.acl

import play.api.mvc.{Results, ActionBuilder, Result, Request}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 15:42
 * Description: Поддержка валидации best-before на реквестах.
 * Это позволяет защититься от повторения этого реквеста в будущем.
 * Применяется совместно с цифровой подписью на аргументах запроса.
 */
object BestBefore {

  /** Текущее время в секундах. */
  def nowSec = System.currentTimeMillis() / 1000L

}



/** Аддон для action builder'ов, позволяющий ограничить срок годности запроса через параметр. */
trait BestBeforeSec[T[_]] extends ActionBuilder[T] {
  /** Срок годности (best before). */
  def bestBeforeSec: Long

  abstract override def invokeBlock[A](request: Request[A], block: (T[A]) => Future[Result]): Future[Result] = {
    if (bestBeforeSec <= BestBefore.nowSec) {
      super.invokeBlock(request, block)
    } else {
      requestExpired(request)
    }
  }

  /** Что делать, если реквест истёк? */
  def requestExpired(request: Request[_]): Future[Result] = {
    val res = Results.RequestTimeout("Request expired.")
    Future successful res
  }
}
