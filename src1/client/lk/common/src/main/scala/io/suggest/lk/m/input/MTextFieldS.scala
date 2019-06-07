package io.suggest.lk.m.input

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
object MTextFieldS {

  implicit object MEpwTextFieldSFastEq extends FastEq[MTextFieldS] {
    override def eqv(a: MTextFieldS, b: MTextFieldS): Boolean = {
      (a.value ===* b.value) &&
      (a.isValid ==* b.isValid)
    }
  }

  @inline implicit def univEq: UnivEq[MTextFieldS] = UnivEq.derive

  def empty = apply()

  val value     = GenLens[MTextFieldS]( _.value )
  val isValid   = GenLens[MTextFieldS]( _.isValid )

}


/** Контейнер данных состояния одного текстовое поле (имени или пароля).
  *
  * @param value Текущее значение.
  * @param isValid Есть ли ошибка?
  */
case class MTextFieldS(
                        value        : String         = "",
                        isValid      : Boolean        = true,
                      ) {

  def isValidNonEmpty: Boolean =
    isValid && value.nonEmpty

}

