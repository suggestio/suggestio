package io.suggest.model.n2.node

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.common.menum.play.EnumJsonReadsT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 11:51
 * Description: Синхронная статическая модель типов узлов N2: карточка, adn-узел, тег, картинка, видео, и т.д.
 * В рамках зотоника была динамическая модель m_category.
 * В рамках s.io нет нужды в такой тяжелой модели, т.к. от категорий мы уже ушли к тегам.
 */
object MNodeTypes extends EnumMaybeWithName with EnumJsonReadsT {

  protected[this] abstract sealed class Val(val strId: String)
    extends super.Val(strId)
  {
    /** Подтипы этого типа. */
    def subTypes: List[T]
  }

  override type T = Val

  /** Реализация Val без подтипов. */
  private class ValNoSub(strId: String) extends Val(strId) {
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
  val Media       = new Val("m") {

    /** Загруженная картинка. */
    val Image: T  = new ValNoSub("i")

    override def subTypes = List[T](Image)

  }

}
