package io.suggest.lk.m.input

import diode.FastEq
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.06.19 13:24
  * Description: Расширенное состояние текстового поля, когда некоторые данные пробрасываются из вышележащей формы.
  */

object MTextFieldExtS {

  implicit object MTextFieldExtSFastEq extends FastEq[MTextFieldExtS] {
    override def eqv(a: MTextFieldExtS, b: MTextFieldExtS): Boolean = {
      (a.typed ===* b.typed) &&
      (a.disabled ==* b.disabled)
    }
  }

  @inline implicit def univEq: UnivEq[MTextFieldExtS] = UnivEq.derive

}


case class MTextFieldExtS(
                           typed   : MTextFieldS,
                           disabled   : Boolean,
                         )
