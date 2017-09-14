package io.suggest.ad.edit.m.edit

import diode.FastEq
import io.suggest.model.n2.node.meta.colors.MColorData

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
      a.colorPresets eq b.colorPresets
    }
  }

}


case class MColorsState(
                         // TODO Надо бы карту презетов для разных картинок.
                         colorPresets    : List[MColorData]     = Nil
                       ) {

  def withColorPresets(colorPresets: List[MColorData]) = copy(colorPresets = colorPresets)

}

