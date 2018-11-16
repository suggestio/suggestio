package io.suggest.adn.edit.m

import diode.FastEq
import io.suggest.color.MColorType
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.04.18 11:21
  * Description: Модель состояния открытого color-picker'а в форме редактора adn-узла.
  */
object MAdnEditColorPickerS {

  implicit object MAdnEditColorPickerSFastEq extends FastEq[MAdnEditColorPickerS] {
    override def eqv(a: MAdnEditColorPickerS, b: MAdnEditColorPickerS): Boolean = {
      (a.ofColorType ===* b.ofColorType) &&
        (a.topLeftPx ===* b.topLeftPx)
    }
  }

  @inline implicit def univEq: UnivEq[MAdnEditColorPickerS] = UnivEq.derive

}


/** Состояние текущего открытого color-picker'а.
  *
  * @param ofColorType Какой именно цвет сейчас редактируется?
  * @param topLeftPx Координата открытого picker'а.
  */
case class MAdnEditColorPickerS(
                                 ofColorType  : MColorType,
                                 topLeftPx    : MCoords2di
                               )
