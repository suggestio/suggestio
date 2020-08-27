package io.suggest.id.login.m.pwch

import diode.FastEq
import io.suggest.lk.m.input.MTextFieldS
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.07.19 19:25
  * Description: Состояние подформы выставления нового пароля.
  * Общая модель для формы смена пароля и для последнего шага регистрации.
  */
object MPwNew {

  def empty = apply()

  implicit object MPwNewFastEq extends FastEq[MPwNew] {
    override def eqv(a: MPwNew, b: MPwNew): Boolean = {
      (a.password1 ===* b.password1) &&
      (a.password2 ===* b.password2) &&
      (a.showPwMisMatch ==* b.showPwMisMatch) &&
      (a.pwVisible ==* b.pwVisible)
    }
  }

  def password1 = GenLens[MPwNew](_.password1)
  def password2 = GenLens[MPwNew](_.password2)
  def showPwMisMatch = GenLens[MPwNew](_.showPwMisMatch)
  def pwVisible = GenLens[MPwNew](_.pwVisible)

  @inline implicit def univEq: UnivEq[MPwNew] = UnivEq.derive

}

/** @param password1 Поле ввода пароля.
  * @param password2 Поле повторения пароля.
  * @param showPwMisMatch Отображать ли юзеру неСовпадение двух паролей?
  */
case class MPwNew(
                   password1            : MTextFieldS             = MTextFieldS.empty,
                   password2            : MTextFieldS             = MTextFieldS.empty,
                   showPwMisMatch       : Boolean                 = false,
                   pwVisible            : Boolean                 = false,
                 ) {

  def isPasswordsMatch: Boolean =
    password1.value ==* password2.value

  lazy val isPasswordsErrorShown: Boolean =
    showPwMisMatch && !isPasswordsMatch

  def canSubmit: Boolean = {
    (password1 :: password2 :: Nil).forall(_.isValid) &&
    isPasswordsMatch &&
    password1.value.nonEmpty
  }

  def passwordValue: String = {
    require( isPasswordsMatch )
    password1.value
  }

}
