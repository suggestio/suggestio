package io.suggest.color

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import io.suggest.model.PrefixedFn
import io.suggest.sc.ScConstants
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.04.18 11:11
  * Description: Модель типов цветов.
  */
object MColorTypes extends StringEnum[MColorType] {

  /** Цвет фона. */
  case object Bg extends MColorType("b") with IColorPickerMarker {
    override def scDefaultHex = ScConstants.Defaults.BG_COLOR
  }

  /** Цвет контента. */
  case object Fg extends MColorType("f") with IColorPickerMarker {
    override def scDefaultHex = ScConstants.Defaults.FG_COLOR
  }


  override def values = findValues

  def scDefaultColors = MColors(
    bg = Some( MColorData(Bg.scDefaultHex) ),
    fg = Some( MColorData(Fg.scDefaultHex) )
  )

}


sealed abstract class MColorType(override val value: String) extends StringEnumEntry {

  /** Дефолтовый цвет для выдачи. */
  def scDefaultHex: String

}

object MColorType {

  implicit def mColorTypeFormat: Format[MColorType] = {
    EnumeratumUtil.valueEnumEntryFormat( MColorTypes )
  }

  @inline implicit def univEq: UnivEq[MColorType] = UnivEq.derive


  implicit class ColorTypeOpsExt( val mct: MColorType ) extends AnyVal {

    def COLOR_CODE_FN = PrefixedFn.fullFn(mct.value, MColorData.Fields.CODE_FN)

  }

}


/** Интерфейс для маркирования текущего color-picker'а в формах, где несколько picker'ов. */
trait IColorPickerMarker extends Product
object IColorPickerMarker {
  @inline implicit def univEq: UnivEq[IColorPickerMarker] = UnivEq.force
}

