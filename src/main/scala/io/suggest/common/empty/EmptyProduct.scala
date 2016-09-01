package io.suggest.common.empty

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 21:19
 * Description: Тестирование case class'а на наполненность параметров.
 */
trait EmptyProduct extends IsEmpty { this: Product =>

  /** @return true, если класс содержит хотя бы одно значение. */
  override def nonEmpty: Boolean = {
    productIterator.exists {
      case opt: Option[_]           => opt.nonEmpty
      case col: TraversableOnce[_]  => col.nonEmpty
      case _                        => true
    }
  }

  /** @return true, если класс не содержит ни одного значения. */
  override final def isEmpty = super.isEmpty

}

