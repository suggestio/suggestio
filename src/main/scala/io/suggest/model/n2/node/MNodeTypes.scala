package io.suggest.model.n2.node

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.common.menum.play.EnumJsonReadsValT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 11:51
 * Description: Синхронная статическая модель типов узлов N2: карточка, adn-узел, тег, картинка, видео, и т.д.
 * В рамках зотоника была динамическая модель m_category.
 * В рамках s.io нет нужды в такой тяжелой модели, т.к. от категорий мы уже ушли к тегам.
 */
object MNodeTypes extends EnumMaybeWithName with EnumJsonReadsValT {

  /** Трейт каждого элемента данной модели. */
  protected sealed trait ValT { that: T =>
    /** Уникальный строковой ключ элемента. */
    def strId: String

    /** Подтипы этого типа. */
    def subTypes: List[T]

    /** Родительский элемент, если есть. */
    def parent: Option[T]

    /** Является ли текущий элемент дочерним по отношению к указанному? */
    def hasParent(ntype: T): Boolean = {
      parent.exists { p =>
        p == ntype || p.hasParent(ntype)
      }
    }
    /** Короткий враппер для hasParent(). */
    def >>(ntype: T) = hasParent(ntype)

    /** Является ли текущий элемент указанным или дочерним? */
    def eqOrHasParent(ntype: T): Boolean = {
      this == ntype || hasParent(ntype)
    }
    /** Короткий враппер к eqOrHasParent(). */
    def ==>>(ntype: T) = eqOrHasParent(ntype)
  }

  /** Абстрактная класс одного элемента модели. */
  protected[this] abstract sealed class Val(val strId: String)
    extends super.Val(strId)
    with ValT


  override type T = Val

  protected sealed trait NoParent extends ValT { that: T =>
    override def parent: Option[T] = None
  }

  /** Реализация Val без подтипов. */
  private class ValNoSub(strId: String) extends Val(strId) with NoParent {
    override def subTypes: List[T] = Nil
  }

  // Элементы дерева типов N2-узлов.

  /** Юзер. */
  val Person: T   = new ValNoSub("p")

  /** Узел ADN. */
  val AdnNode: T  = new ValNoSub("n")

  /** Рекламная карточка. */
  val Ad: T       = new ValNoSub("a")

  /** Теги/ключевые слова. */
  val Tag: T      = new ValNoSub("t")

  /** Картинки, видео и т.д. */
  val Media       = new Val("m") with NoParent { that =>

    private trait _Parent extends ValNoSub {
      override def parent: Option[T] = Some(that)
    }

    /** Загруженная картинка. */
    val Image: T  = new ValNoSub("i") with _Parent

    override def subTypes = List[T](Image)

  }

}
