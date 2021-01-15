package io.suggest.lk.m.color

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

  @inline implicit def univEq: UnivEq[MColorsState] = UnivEq.derive


  /** Макс.длина списка презетов. Должна быть кратна 8 (зависит от color-picker'а). */
  private def PRESETS_LEN_MAX = 16

  def prependPresets(presets0: List[MColorData], mcd: MColorData): List[MColorData] = {
    var cps = mcd :: presets0
    if (cps.lengthCompare(PRESETS_LEN_MAX) > 0)
      cps = cps.slice(0, PRESETS_LEN_MAX)
    cps
  }

  def colorPresets  = GenLens[MColorsState](_.colorPresets)
  def histograms    = GenLens[MColorsState](_.histograms)
  def picker        = GenLens[MColorsState](_.picker)


  implicit final class McsExt( private val mcs0: MColorsState ) extends AnyVal {

    def prependPresets(mcd: MColorData): MColorsState = {
      MColorsState.colorPresets
        .composeLens( MHistogram.colors )
        .modify( MColorsState.prependPresets(_, mcd) )(mcs0)
    }

  }

}


/** Модель состояния цветов.
  *
  * @param colorPresets Избранные цвета.
  * @param histograms nodeId картинки -> MHistogram
  * @param picker Состояние текущего открытого color-picker'а.
  *               None - color picker отсутствует.
  */
case class MColorsState(
                         colorPresets    : MHistogram                 = MHistogram.empty,
                         histograms      : Map[String, MHistogram]    = Map.empty,
                         picker          : Option[MColorPickerS]      = None,
                       ) {

  lazy val colorPresetsLen = colorPresets.colors.length

}

