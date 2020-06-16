package io.suggest.id.login.m.reg.step4

import diode.FastEq
import diode.data.Pot
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
      (a.submitReq ===* b.submitReq)
    }
  }

  def submitReq = GenLens[MReg4SetPassword](_.submitReq)

  @inline implicit def univEq: UnivEq[MReg4SetPassword] = UnivEq.derive

}


/** Состояние выставления первого пароля для нового юзера.
  *
  * @param submitReq Pot финального сабмита.
  */
case class MReg4SetPassword(
                             submitReq        : Pot[MRegTokenResp]      = Pot.empty,
                           ) {

  def canSubmit: Boolean = {
    !submitReq.isPending
  }

}
