package io.suggest.lk.nodes.form.m

import diode.FastEq
import diode.data.Pot
import io.suggest.bill.tf.daily.ITfDailyMode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.17 21:48
  * Description: Модель состояния редактирования тарифа текущего узла.
  */
object MEditTfDailyS {

  /** Поддержка FastEq. */
  implicit object MTfDailyEditSFastEq extends FastEq[MEditTfDailyS] {
    override def eqv(a: MEditTfDailyS, b: MEditTfDailyS): Boolean = {
      (a.mode eq b.mode) &&
        (a.request eq b.request)
    }
  }

}

case class MEditTfDailyS(
                          mode    : ITfDailyMode,
                          request : Pot[_] = Pot.empty
                        )
