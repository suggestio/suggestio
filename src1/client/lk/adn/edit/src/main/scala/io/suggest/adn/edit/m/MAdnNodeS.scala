package io.suggest.adn.edit.m

import diode.FastEq
import io.suggest.color.MColorData
import io.suggest.common.geom.coord.MCoords2di
import io.suggest.model.n2.node.meta.MMetaPub
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 17:37
  * Description: Модель состояния редактирования данных узла.
  */
object MAdnNodeS {

  implicit object MAdnNodeSFastEq extends FastEq[MAdnNodeS] {
    override def eqv(a: MAdnNodeS, b: MAdnNodeS): Boolean = {
      (a.meta ===* b.meta) &&
        (a.colorPresets ===* b.colorPresets) &&
        (a.colorPicker ===* b.colorPicker)
    }
  }

  implicit def univEq: UnivEq[MAdnNodeS] = UnivEq.derive

}


/** Модель состояния редактирования узла.
  *
  * @param meta Текстовые метаданные узла.
  */
case class MAdnNodeS(
                      meta          : MMetaPub,
                      colorPresets  : List[MColorData],
                      colorPicker   : Option[MCoords2di] = None
                      // TODO js-эджи для картинок.
                    )
