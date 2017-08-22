package io.suggest.font

import enumeratum.values.{IntEnum, IntEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import play.api.libs.json.Format


/** Статическая поддержка для элементов модели [[MFontSize]]. */
object MFontSize {

  /** Поддержка play-json. */
  implicit val FONT_SIZE_FORMAT: Format[MFontSize] = {
    EnumeratumUtil.valueEnumEntryFormat( MFontSizes )
  }

}

/** Класс модели размера шрифта.
  *
  * @param value CSS font-size.
  */
sealed abstract class MFontSize(override val value: Int) extends IntEnumEntry {

  private def _lineHeightDiff: Int = if (value >= 32) 4 else 2

  /** CSS line-height */
  def lineHeight: Int = value - _lineHeightDiff

  def isLast: Boolean = false

}


/** Модель допустимых размеров шрифтов и инфа по ним. */
object MFontSizes extends IntEnum[MFontSize] {

  // Значения модели.

  case object F10 extends MFontSize(10)
  case object F12 extends MFontSize(12)
  case object F14 extends MFontSize(14)
  case object F16 extends MFontSize(16)
  case object F18 extends MFontSize(18)
  case object F22 extends MFontSize(22)
  case object F26 extends MFontSize(26)
  case object F30 extends MFontSize(30)
  case object F34 extends MFontSize(34)
  case object F38 extends MFontSize(38)
  case object F42 extends MFontSize(42)
  case object F46 extends MFontSize(46)
  case object F50 extends MFontSize(50)
  case object F54 extends MFontSize(54)
  case object F58 extends MFontSize(58)
  case object F62 extends MFontSize(62)
  case object F66 extends MFontSize(66)
  case object F70 extends MFontSize(70)
  case object F74 extends MFontSize(74)
  case object F80 extends MFontSize(80)
  case object F84 extends MFontSize(84) {
    override def isLast = true
  }


  override val values = findValues


  def min: MFontSize = values.head
  def max: MFontSize = values.last

}
