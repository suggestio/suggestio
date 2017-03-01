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
    productIterator
      .exists { EmptyProduct.nonEmpty }
  }

  /** @return true, если класс не содержит ни одного значения. */
  override final def isEmpty = super.isEmpty

  /** Опционально вернуть this.
    * @return Если данных нет (!nonEmpty), то будет None.
    *         Если хоть какие-то данные есть, то Some(this).
    */
  def optional: Option[this.type] = {
    if (nonEmpty) Some(this) else None
  }

}

object EmptyProduct {

  def nonEmpty(v: Any): Boolean = {
    v match {
      case opt: Option[_]           => opt.nonEmpty
      case col: TraversableOnce[_]  => col.nonEmpty
      case m: INonEmpty             => m.nonEmpty
      case m: IIsEmpty              => !m.isEmpty
      case _                        => true
    }
  }

}


