package io.suggest.lk.m.color

import diode.FastEq
import io.suggest.color.{MColorData, MHistogram}
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.09.17 15:02
  * Description: Модель состояния цветов, пошарена между color picker'ами.
  */
object MColorsState {

  def empty = apply()

  implicit object MColorsDataSFastEq extends FastEq[MColorsState] {
    override def eqv(a: MColorsState, b: MColorsState): Boolean = {
      (a.colorPresets ===* b.colorPresets) &&
      (a.histograms ===* b.histograms) &&
      (a.picker ===* b.picker)
    }
  }

  @inline implicit def univEq: UnivEq[MColorsState] = UnivEq.derive


  /** Макс.длина списка презетов. Должна быть кратна 8 (зависит от color-picker'а). */
  private val PRESETS_LEN_MAX = 16

  def prependPresets(presets0: List[MColorData], mcd: MColorData): List[MColorData] = {
    var cps = mcd :: presets0
    if (cps.lengthCompare(PRESETS_LEN_MAX) > 0)
      cps = cps.slice(0, PRESETS_LEN_MAX)
    cps
  }

  def colorPresets  = GenLens[MColorsState](_.colorPresets)
  def histograms    = GenLens[MColorsState](_.histograms)
  def picker        = GenLens[MColorsState](_.picker)

}


/** Модель состояния цветов.
  *
  * @param colorPresets Избранные цвета.
  * @param histograms nodeId картинки -> MHistogram
  * @param picker Состояние текущего открытого color-picker'а.
  *               None - color picker отсутствует.
  */
case class MColorsState(
                         colorPresets    : List[MColorData]           = Nil,
                         histograms      : Map[String, MHistogram]    = Map.empty,
                         picker          : Option[MColorPickerS]      = None,
                       ) {

  def withColorPresets(colorPresets: List[MColorData])        = copy(colorPresets = colorPresets)
  def withHistograms(histograms: Map[String, MHistogram])     = copy(histograms = histograms)
  def withPicker(picker: Option[MColorPickerS])               = copy(picker = picker)

  lazy val colorPresetsLen = colorPresets.length

  def prependPresets(mcd: MColorData) = withColorPresets( MColorsState.prependPresets(colorPresets, mcd) )

}

