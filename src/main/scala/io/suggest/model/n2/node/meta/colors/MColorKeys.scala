package io.suggest.model.n2.node.meta.colors

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.common.menum.play.EnumJsonReadsValT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 16:19
 * Description: Модель-словарь цветов. Используется как ключи для хранимой модели-карты цветов.
 */
object MColorKeys extends EnumMaybeWithName with EnumJsonReadsValT {

  /** Класс одного элемента модели. */
  protected[this] sealed class Val(val strId: String)
    extends super.Val(strId)

  override type T = Val

  /** Цвет фона / заднего плана. */
  val Bg        : T = new Val("b")

  /** Цвета элементов переднего плана поверх фона (текста, svg-иконок, и т.д.). */
  val Fg        : T = new Val("f")

  /** Цвет покрывающего паттерна.
    * Изначально (2015) использовался только в карточках. */
  val Pattern   : T = new Val("p")

}

