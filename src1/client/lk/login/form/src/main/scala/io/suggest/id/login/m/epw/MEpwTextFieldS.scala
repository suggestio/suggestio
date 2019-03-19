package io.suggest.id.login.m.epw

import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.03.19 16:44
  * Description: Модель данных состояния текстового поля ввода имени или пароля.
  */
object MEpwTextFieldS {

  implicit object MEpwTextFieldSFastEq extends FastEq[MEpwTextFieldS] {
    override def eqv(a: MEpwTextFieldS, b: MEpwTextFieldS): Boolean = {
      (a.value ===* b.value)
    }
  }

  @inline implicit def univEq: UnivEq[MEpwTextFieldS] = UnivEq.derive

  def empty = apply()

  val value = GenLens[MEpwTextFieldS](_.value)

}


/** Контейнер данных состояния одного текстовое поле (имени или пароля).
  *
  * @param value Текущее значение.
  */
case class MEpwTextFieldS(
                           value        : String         = "",
                         )
