package io.suggest.common.menum

import scala.collection.AbstractIterator

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.10.15 11:09
 * Description: Утиль для упрощенной сборки enumeration, имеющих нелинейную внутреннюю структуру.
 */
trait EnumTree extends IVeryLightEnumeration {

  override type T <: ValT

  /** Трейт каждого элемента данной модели. */
  protected trait ValT extends super.ValT { that: T =>

    /** Уникальный строковой ключ элемента. */
    def strId: String

    /** Подтипы этого типа. */
    def children: List[T]

    /** Родительский элемент, если есть. */
    def parent: Option[T]

    /** Является ли текущий элемент дочерним по отношению к указанному? */
    def hasParent(ntype: T): Boolean = {
      parent.exists { p =>
        p == ntype || p.hasParent(ntype)
      }
    }
    /** Короткий враппер для hasParent(). */
    def >>(ntype: T): Boolean = {
      hasParent(ntype)
    }

    /** Является ли текущий элемент указанным или дочерним? */
    def eqOrHasParent(ntype: T): Boolean = {
      this == ntype || hasParent(ntype)
    }
    /** Короткий враппер к eqOrHasParent(). */
    def ==>>(ntype: T): Boolean = {
      eqOrHasParent(ntype)
    }

    private abstract class _Iterator extends AbstractIterator[T] {
      var _parentOpt: Option[T]

      override def hasNext: Boolean = {
        _parentOpt.isDefined
      }

      override def next(): T = {
        val res = _parentOpt.get
        _parentOpt = res.parent
        res
      }
    }

    /** Итератор родительских элементов. */
    def parentsIterator: Iterator[T] = {
      new _Iterator {
        override var _parentOpt: Option[T] = parent
      }
    }

    /** Итератор из текущего и родительских элементов. */
    def meAndParentsIterator: Iterator[T] = {
      new _Iterator {
        override var _parentOpt: Option[T] = Some(that)
      }
    }

  }

}
