package io.suggest.sjs.common.model

import scala.util.Try

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.08.15 17:48
 * Description: Контейнер для возврата timestamped-результатов.
 */
trait Timestamped[T] {
  def result: Try[T]
  def timestamp: Long
}

trait TimestampedCompanion[T] {
  def apply(result: Try[T], timestamp: Long): Timestamped[T]
}
