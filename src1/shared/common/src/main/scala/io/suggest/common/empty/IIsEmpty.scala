package io.suggest.common.empty

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

trait IIsNonEmpty
  extends IIsEmpty
  with INonEmpty


trait NonEmpty extends IIsNonEmpty {
  override def nonEmpty = !isEmpty
}

trait IsEmpty extends IIsNonEmpty {
  override def isEmpty = !nonEmpty
}
