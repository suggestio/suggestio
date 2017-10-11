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

}


case class MColorsState(
                         // TODO Надо бы карту презетов для разных картинок.
                         colorPresets    : List[MColorData]           = Nil,
                         histograms      : Map[String, MHistogram]    = Map.empty
                       ) {

  def withColorPresets(colorPresets: List[MColorData])        = copy(colorPresets = colorPresets)
  def withHistograms(histograms: Map[String, MHistogram])     = copy(histograms = histograms)

}

