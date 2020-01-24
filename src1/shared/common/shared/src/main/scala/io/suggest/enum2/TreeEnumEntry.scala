package io.suggest.enum2

import japgolly.univeq._

import scala.collection.immutable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.08.17 17:46
  * Description: Для сборки enumeratum'ов с древовидными внутренностями можно подмешать этот трейт
  * к базовому классу элементов enumeratum-модели.
  */
object TreeEnumEntry {

  /** Макрос findValues собирает только значения на верхнем уровне.
    * Для подключения подуровней, надо результат макроса передать в этот метод,
    * и получится правильное множество элементов со всех уровней дерева.
    *
    * @param findValuesRes Результат вызова макроса findValues.
    * @tparam T Тип одного значения модели, реализующего TreeEnumEntry.
    * @return Последовательность, включающая в себя элементы со всех подуровней.
    */
  def deepFindValue[T <: TreeEnumEntry[T]](findValuesRes: immutable.IndexedSeq[T]): immutable.IndexedSeq[T] = {
    findValuesRes
      .flatMap { v =>
        v #:: v.deepChildren
      }
  }


  implicit final class TreeEnumEntryOpsExt[T <: TreeEnumEntry[T]]( private val that: T ) extends AnyVal {

    /** Вернуть все дочерние элементы с вообще всех подуровней. */
    def deepChildren: LazyList[T] = {
      that
        .children
        .flatMap { v =>
          v #:: v.deepChildren
        }
    }

    /** Является ли текущий элемент дочерним по отношению к указанному? */
    def hasParent(ntype: T)(implicit univEq: UnivEq[T]): Boolean = {
      that.parent.exists { p =>
        (p ==* ntype) || p.hasParent(ntype)
      }
    }
    /** Короткий враппер для hasParent(). */
    def >>(ntype: T)(implicit univEq: UnivEq[T]) = hasParent(ntype)

    /** Является ли текущий элемент указанным или дочерним? */
    def eqOrHasParent(ntype: T)(implicit univEq: UnivEq[T]): Boolean = {
      (that ==* ntype) || hasParent(ntype)
    }
    /** Короткий враппер к eqOrHasParent(). */
    def ==>>(ntype: T)(implicit univEq: UnivEq[T]) =
      eqOrHasParent(ntype)


    /** Итератор родительских элементов. */
    def parents: LazyList[T] = {
      LazyList.unfold(that) { that2 =>
        for (parent2 <- that2.parent)
        yield (parent2, parent2)
      }
    }

    /** Итератор из текущего и родительских элементов. */
    def meAndParents: LazyList[T] =
      that #:: parents

  }

}


trait TreeEnumEntry[T <: TreeEnumEntry[T]] { that: T =>

  /** Подтипы этого типа. */
  def children: LazyList[T] = LazyList.empty

  /** Родительский элемент, если есть. */
  def parent: Option[T] = None

  /** Трейт для дочерних элементов. Они обычно наследуют черты родителей. */
  trait _Child { child: T =>
    override def parent = Some(that)
  }

}
