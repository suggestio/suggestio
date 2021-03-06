package io.suggest.font

import enumeratum.values.{IntEnum, IntEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format


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
  case object F84 extends MFontSize(84)
  case object F90 extends MFontSize(90)
  case object F100 extends MFontSize(100)
  case object F110 extends MFontSize(110)
  case object F120 extends MFontSize(120)
  case object F130 extends MFontSize(130)
  case object F140 extends MFontSize(140)
  case object F150 extends MFontSize(150)
  case object F170 extends MFontSize(170)
  case object F200 extends MFontSize(200)
  case object F220 extends MFontSize(220)
  case object F240 extends MFontSize(240)
  case object F260 extends MFontSize(260)
  case object F300 extends MFontSize(300)

  override val values = findValues

  def min: MFontSize = values.head
  def max: MFontSize = values.last


  /** Дефолтовое значение. */
  def default = F18

}


/** Класс модели размера шрифта.
  *
  * @param value CSS font-size.
  */
sealed abstract class MFontSize(override val value: Int) extends IntEnumEntry {

  override final def hashCode = value

  override final def toString = value.toString

}


/** Статическая поддержка для элементов модели [[MFontSize]]. */
object MFontSize {

  /** Поддержка play-json. */
  implicit val FONT_SIZE_FORMAT: Format[MFontSize] = {
    EnumeratumUtil.valueEnumEntryFormat( MFontSizes )
  }

  @inline implicit def univEq: UnivEq[MFontSize] = UnivEq.derive


  implicit class FontSizeOpsExt( val fs: MFontSize ) extends AnyVal {

    /** В HTML5 задан принудительный минимальный line-height для inline-элементов.
      * И это сказывается на мелких шрифтах: межстрочка не поспевает, и получаются большие интервалы
      * между соседними строками.
      *
      * Чтобы это исправить, надо помечать тексты как display:block.
      *
      * @return true, если пора фиксить местрочку
      */
    def forceRenderBlockHtml5: Boolean = {
      fs.value < 18
    }

    /** CSS line-height */
    def lineHeight: Int =
      fs.value - (if (fs.value >= 32) 4 else 2)

  }

}

