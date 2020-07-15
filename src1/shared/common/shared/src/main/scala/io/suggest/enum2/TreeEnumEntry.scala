package io.suggest.enum2

import japgolly.univeq._

import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.08.17 17:46
  * Description: Для сборки enumeratum'ов с древовидными внутренностями можно подмешать этот трейт
  * к базовому классу элементов enumeratum-модели.
  */
object TreeEnumEntry {

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


  private def _mostChildestOf[T <: TreeEnumEntry[T]: UnivEq](el0: T, el1: T): Option[T] = {
    if (el1 hasParent el0) {
      Some( el1 )
    } else if (el0 hasParent el1) {
      Some( el0 )
    } else {
      // Разные предикаты в одном поле? Такого быть не должно.
      //throw new IllegalArgumentException(s"dedupParentChild(): At least two elements have no direct parent-child relation: $el0 - $el1")
      None
    }
  }

  /** Коллекция из tree-элементов. */
  implicit final class TreeEnumEntriesOpsExt[T <: TreeEnumEntry[T]]( private val thatColl: IterableOnce[T] ) extends AnyVal {

    /** Дедубликация списка из **родственных** предикатов до одного элемента, наиболее дочернего из всех.
      *
      * Это простой, но небезопасный метод, сыплящий ошибками при любых неточностях в исходных данных.
      *
      * @throws IllegalArgumentException Если в списке есть элементы, не состоящие в линии прямого родства.
      * @return Some() с наиболее child-предикатом.
      *         None, если исходный список пуст.
      */
    def mostChildestOneUnsafe(implicit univEq: UnivEq[T]): Option[T] = {
      thatColl
        .iterator
        .reduceOption { (el0, el1) =>
          _mostChildestOf( el0, el1) getOrElse {
            // Разные предикаты в одном поле? Такого быть не должно.
            throw new IllegalArgumentException(s"dedupParentChild(): At least two elements have no direct parent-child relation: $el0 - $el1")
          }
        }
    }


    /** Убрать родительские элементы при наличии дочерних.
      *
      * @return Обновлённая коллекция в неопределённом порядке.
      */
    def mostChildest(implicit univEq: UnivEq[T]): List[T] = {
      thatColl
        .iterator
        .foldLeft( List.empty[T] ) { (acc0, el2) =>
          (for {
            (el1, i) <- acc0
              .iterator
              .zipWithIndex
            elX <- _mostChildestOf( el1, el2 )
            isChanged = (el1 ===* elX)
          } yield {
            if (isChanged)
              // удалить старый элемент, добавив новый.
              acc0.updated( i, elX )
            else
              acc0
          })
            .nextOption()
            .getOrElse {
              el2 :: acc0
            }
        }
    }

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
