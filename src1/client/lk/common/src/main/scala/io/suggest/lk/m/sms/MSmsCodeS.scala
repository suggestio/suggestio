package io.suggest.lk.m.sms

import diode.FastEq
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.06.19 11:05
  * Description: Модель состояния ввода смс-кода.
  */
object MSmsCodeS {

  def empty = apply()

  implicit object MSmsCodeSFastEq extends FastEq[MSmsCodeS] {
    override def eqv(a: MSmsCodeS, b: MSmsCodeS): Boolean = {
      (a.typed ===* b.typed)
    }
  }

  @inline implicit def univEq: UnivEq[MSmsCodeS] = UnivEq.derive

  val typed = GenLens[MSmsCodeS](_.typed)

}


/** Контейнер данных состояния формы ввода смс-кода.
  *
  * @param typed Состояние текстового инпута.
  */
case class MSmsCodeS(
                      typed               : MTextFieldS           = MTextFieldS.empty,
                    )
