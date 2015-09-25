package io.suggest.common

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 21:19
 * Description: Тестирование case class'а на наполненность параметров.
 */
trait EmptyProduct extends Product {

  /** @return true, если класс содержит хотя бы одно значение. */
  def nonEmpty: Boolean = {
    productIterator.exists {
      case opt: Option[_]           => opt.nonEmpty
      case col: TraversableOnce[_]  => col.nonEmpty
      case _                        => true
    }
  }

  /** @return true, если класс не содержит ни одного значения. */
  final def isEmpty: Boolean = !nonEmpty

}

