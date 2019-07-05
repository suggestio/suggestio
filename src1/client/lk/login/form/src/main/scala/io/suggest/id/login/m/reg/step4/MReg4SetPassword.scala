package io.suggest.id.login.m.reg.step4

import diode.FastEq
import diode.data.Pot
import io.suggest.id.login.m.pwch.{IPwNewSubmit, MPwNew}
import io.suggest.id.login.m.reg.ICanSubmit
import io.suggest.id.reg.MRegTokenResp
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
      (a.pwNew ===* b.pwNew) &&
      (a.submitReq ===* b.submitReq)
    }
  }

  val pwNew = GenLens[MReg4SetPassword](_.pwNew)
  val submitReq = GenLens[MReg4SetPassword](_.submitReq)

  @inline implicit def univEq: UnivEq[MReg4SetPassword] = UnivEq.derive

}


/** Состояние выставления первого пароля для нового юзера.
  *
  * @param pwNew Состояние инпутов нового пароля.
  * @param submitReq Pot финального сабмита.
  */
case class MReg4SetPassword(
                             pwNew            : MPwNew                  = MPwNew.empty,
                             submitReq        : Pot[MRegTokenResp]      = Pot.empty,
                           )
  extends ICanSubmit
  with IPwNewSubmit
{

  override def canSubmit: Boolean = {
    pwNew.canSubmit &&
    !submitReq.isPending
  }

}
