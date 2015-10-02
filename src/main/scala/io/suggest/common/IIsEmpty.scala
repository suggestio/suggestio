package io.suggest.common

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.15 14:58
 * Description: Интерфейсы для очень частых методов isEmpty, nonEmpty.
 */
trait IIsEmpty {
  def isEmpty: Boolean
}

trait INonEmpty {
  def nonEmpty: Boolean
}

trait IEmpty
  extends IIsEmpty
  with INonEmpty


trait NonEmpty extends IEmpty {
  override def nonEmpty = !isEmpty
}

trait IsEmpty extends IEmpty {
  override def isEmpty = !nonEmpty
}
