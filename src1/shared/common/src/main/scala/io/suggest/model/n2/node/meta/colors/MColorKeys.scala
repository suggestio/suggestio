package io.suggest.model.n2.node.meta.colors

import enumeratum.values.{StringEnum, StringEnumEntry}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 16:19
 * Description: Модель-словарь цветов. Используется как ключи для хранимой модели-карты цветов.
 */

object MColorKeys extends StringEnum[MColorKey] {

  /** Цвет фона / заднего плана. */
  case object Bg extends MColorKey("b")

  /** Цвета элементов переднего плана поверх фона (текста, svg-иконок, и т.д.). */
  case object Fg extends MColorKey("f")

  /** Цвет покрывающего паттерна.
    * Изначально (2015) использовался только в карточках. */
  case object Pattern extends MColorKey("p")


  override val values = findValues

}


sealed abstract class MColorKey(override val value: String) extends StringEnumEntry {
  override final def toString = value
}

