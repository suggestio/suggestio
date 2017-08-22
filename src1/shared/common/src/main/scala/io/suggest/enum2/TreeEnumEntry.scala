package io.suggest.enum2

import scala.collection.AbstractIterator

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.08.17 17:46
  * Description: Для сборки enumeratum'ов с древовидными внутренностями можно подмешать этот трейт
  * к базовому классу элементов enumeratum-модели.
  */
trait TreeEnumEntry[T <: TreeEnumEntry[T]] { that: T =>

  /** Подтипы этого типа. */
  def children: List[T] = Nil

  /** Вернуть все дочерние элементы с вообще всех подуровней. */
  final def deepChildren: List[T] = {
    children.flatMap { v =>
      v :: v.deepChildren
    }
  }

  /** Родительский элемент, если есть. */
  def parent: Option[T] = None

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


  /** Трейт для дочерних элементов. Они обычно наследуют черты родителей. */
  protected trait _Child { child: T =>
    override def parent = Some(that)
  }

}
