package io.suggest.ad.edit.m.edit

import diode.FastEq
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.09.17 22:47
  * Description: Модель props-состояния strip-редактора.
  */
object MStripEdS {

  implicit object MStripEdSFastEq extends FastEq[MStripEdS] {
    override def eqv(a: MStripEdS, b: MStripEdS): Boolean = {
      (a.isLastStrip ==* b.isLastStrip) &&
      (a.confirmingDelete ==* b.confirmingDelete)
    }
  }

  @inline implicit def univEq: UnivEq[MStripEdS] = UnivEq.derive

  val isLastStrip = GenLens[MStripEdS](_.isLastStrip)
  val confirmingDelete = GenLens[MStripEdS](_.confirmingDelete)

}


/** Класс модели пропертисов редактора блока.
  *
  * @param confirmingDelete Отображается подтверждение удаления блока?
  */
case class MStripEdS(
                      isLastStrip                 : Boolean,
                      confirmingDelete            : Boolean           = false
                    )
