package io.suggest.ad.edit.m.edit.color

import diode.FastEq
import io.suggest.color.{MColorData, MHistogram}
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

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
        (a.histograms ===* b.histograms)
    }
  }

  implicit def univEq: UnivEq[MColorsState] = UnivEq.derive


  /** Макс.длина списка презетов. Должна быть кратна 8 (зависит от color-picker'а). */
  private val PRESETS_LEN_MAX = 16

  def prependPresets(presets0: List[MColorData], mcd: MColorData): List[MColorData] = {
    var cps = mcd :: presets0
    if (cps.lengthCompare(PRESETS_LEN_MAX) > 0)
      cps = cps.slice(0, PRESETS_LEN_MAX)
    cps
  }

}


/** Модель состояния цветов.
  *
  * @param colorPresets Избранные цвета.
  * @param histograms nodeId картинки -> MHistogram
  */
case class MColorsState(
                         // TODO Надо бы карту презетов для разных картинок.
                         colorPresets    : List[MColorData]           = Nil,
                         histograms      : Map[String, MHistogram]    = Map.empty
                       ) {

  def withColorPresets(colorPresets: List[MColorData])        = copy(colorPresets = colorPresets)
  def withHistograms(histograms: Map[String, MHistogram])     = copy(histograms = histograms)


  def prependPresets(mcd: MColorData) = withColorPresets( MColorsState.prependPresets(colorPresets, mcd) )

}

