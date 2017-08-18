package io.suggest.common.menum

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 31.08.15 14:28
 * Description: Поддержка поиска элементов enumeration по карте.
 */
trait MaybeWithNameMap extends IMaybeWithName {

  /** Источник значений*/
  def namedValues: TraversableOnce[(String, T)]

  /** Карта элементов. */
  protected def _nameMap: Map[String, T] = {
    namedValues.toMap
  }

  /** Следует сделать override val в финальной реализации. */
  protected def _nameMapVal: Map[String, T]

  override def maybeWithName(n: String): Option[T] = {
    _nameMapVal.get(n)
  }

}



/** Когда у каждого элемента enum'а может быть несколько  */
trait MaybeWithMultiNameMap extends MaybeWithNameMap with ILightEnumeration {

  /** Интерфейс экземпляра модели. */
  protected trait ValT extends super.ValT {
    /** Имена элемента модели в произвольном порядке. */
    def _names: TraversableOnce[String]
  }

  override type T <: ValT

  /** Источник всех значений этой модели. */
  protected def _values: TraversableOnce[T]

  /** Источник значений */
  override def namedValues: TraversableOnce[(String, T)] = {
    _values
      .toIterator
      .flatMap { v =>
        v._names.toIterator
          .map { _ -> v }
      }
  }
}


/** Гибрид scala.Enumeration и [[MaybeWithMultiNameMap]]. */
trait EnumMaybeWithMultiNameMap extends Enumeration with MaybeWithMultiNameMap {

  override protected def _values: TraversableOnce[T] = {
    values.asInstanceOf[TraversableOnce[T]]
  }
}
