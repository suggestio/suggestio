package io.suggest.model.n2.node.meta.colors

import enumeratum._
import io.suggest.primo.IStrId

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 16:19
 * Description: Модель-словарь цветов. Используется как ключи для хранимой модели-карты цветов.
 */

sealed abstract class MColorKey extends EnumEntry with IStrId {
  override final def toString = super.toString
}


object MColorKeys extends Enum[MColorKey] {

  /** Цвет фона / заднего плана. */
  case object Bg extends MColorKey {
    override def strId = "b"
  }

  /** Цвета элементов переднего плана поверх фона (текста, svg-иконок, и т.д.). */
  case object Fg extends MColorKey {
    override def strId = "f"
  }

  /** Цвет покрывающего паттерна.
    * Изначально (2015) использовался только в карточках. */
  case object Pattern extends MColorKey {
    override def strId = "p"
  }


  override val values = findValues

}
