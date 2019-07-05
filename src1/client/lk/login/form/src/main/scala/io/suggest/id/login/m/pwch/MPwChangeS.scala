package io.suggest.id.login.m.pwch

import diode.FastEq
import diode.data.Pot
import io.suggest.id.reg.MRegTokenResp
import io.suggest.lk.m.input.MTextFieldS
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.07.19 19:12
  * Description: Состояние формы смены текущего пароля.
  */
object MPwChangeS {

  implicit object MPwChangeSFastEq extends FastEq[MPwChangeS] {
    override def eqv(a: MPwChangeS, b: MPwChangeS): Boolean = {
      (a.pwOld ===* b.pwOld) &&
      (a.submitReq ===* b.submitReq)
    }
  }

  val oldPw       = GenLens[MPwChangeS](_.pwOld)
  val submitReq   = GenLens[MPwChangeS](_.submitReq)

  @inline implicit def univEq: UnivEq[MPwChangeS] = UnivEq.derive

}


case class MPwChangeS(
                       pwOld        : MTextFieldS             = MTextFieldS.empty,
                       submitReq    : Pot[MRegTokenResp]      = Pot.empty,
                     )
