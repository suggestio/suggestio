package io.suggest.font

import enumeratum.values.{IntEnum, IntEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import play.api.libs.json.Format


/** Статическая поддержка для элементов модели [[FontSize]]. */
object FontSize {

  /** Поддержка play-json. */
  implicit val FONT_SIZE_FORMAT: Format[FontSize] = {
    EnumeratumUtil.valueEnumEntryFormat( FontSizes )
  }

}

/** Класс модели размера шрифта.
  *
  * @param value CSS font-size.
  */
sealed abstract class FontSize(override val value: Int) extends IntEnumEntry {

  private def _lineHeightDiff: Int = if (value >= 32) 4 else 2

  /** CSS line-height */
  def lineHeight: Int = value - _lineHeightDiff

  def isLast: Boolean = false

}


/** Модель допустимых размеров шрифтов и инфа по ним. */
object FontSizes extends IntEnum[FontSize] {

  // Значения модели.

  case object F10 extends FontSize(10)
  case object F12 extends FontSize(12)
  case object F14 extends FontSize(14)
  case object F16 extends FontSize(16)
  case object F18 extends FontSize(18)
  case object F22 extends FontSize(22)
  case object F26 extends FontSize(26)
  case object F30 extends FontSize(30)
  case object F34 extends FontSize(34)
  case object F38 extends FontSize(38)
  case object F42 extends FontSize(42)
  case object F46 extends FontSize(46)
  case object F50 extends FontSize(50)
  case object F54 extends FontSize(54)
  case object F58 extends FontSize(58)
  case object F62 extends FontSize(62)
  case object F66 extends FontSize(66)
  case object F70 extends FontSize(70)
  case object F74 extends FontSize(74)
  case object F80 extends FontSize(80)
  case object F84 extends FontSize(84) {
    override def isLast = true
  }


  override val values = findValues


  def min = values.head
  def max = values.last

}
