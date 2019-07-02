package io.suggest.id.login.m.reg.step4

import diode.FastEq
import diode.data.Pot
import io.suggest.id.login.m.reg.ICanSubmit
import io.suggest.id.reg.MRegTokenResp
import io.suggest.lk.m.input.MTextFieldS
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.06.19 15:50
  * Description: Модель выставления пароля для юзера.
  */
object MReg4SetPassword {

  def empty = apply()

  implicit object MReg4SetPasswordFastEq extends FastEq[MReg4SetPassword] {
    override def eqv(a: MReg4SetPassword, b: MReg4SetPassword): Boolean = {
      (a.password1 ===* b.password1) &&
      (a.password2 ===* b.password2) &&
      (a.submitReq ===* b.submitReq)
    }
  }

  val password1 = GenLens[MReg4SetPassword](_.password1)
  val password2 = GenLens[MReg4SetPassword](_.password2)
  val submitReq = GenLens[MReg4SetPassword](_.submitReq)

  @inline implicit def univEq: UnivEq[MReg4SetPassword] = UnivEq.derive

}


/** Выставление пароля.
  *
  * @param password1 Поле ввода пароля.
  * @param password2 Поле повторения пароля.
  * @param showPwMisMatch Отображать ли юзеру неСовпадение двух паролей?
  * @param submitReq Pot финального сабмита.
  */
case class MReg4SetPassword(
                             password1        : MTextFieldS             = MTextFieldS.empty,
                             password2        : MTextFieldS             = MTextFieldS.empty,
                             showPwMisMatch   : Boolean                 = false,
                             submitReq        : Pot[MRegTokenResp]      = Pot.empty,
                           )
  extends ICanSubmit
{

  def isPasswordsMatch: Boolean =
    password1.value ==* password2.value

  lazy val isPasswordMismatchShown: Boolean =
    showPwMisMatch && !isPasswordsMatch

  override def canSubmit: Boolean = {
    !submitReq.isPending &&
    (password1 :: password2 :: Nil).forall(_.isValid) &&
    isPasswordsMatch &&
    password1.value.nonEmpty
  }

}
