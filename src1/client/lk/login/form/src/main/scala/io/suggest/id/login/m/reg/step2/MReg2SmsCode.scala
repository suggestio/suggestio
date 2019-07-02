package io.suggest.id.login.m.reg.step2

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.EmptyProductPot
import io.suggest.id.login.m.reg.ICanSubmit
import io.suggest.id.reg.MRegTokenResp
import io.suggest.lk.m.sms.MSmsCodeS
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.06.19 18:30
  * Description: Модель состояния второго шага регистрации, где ввод смс-кода.
  */
object MReg2SmsCode {

  def empty = apply()

  implicit object MReg2SmsCodeFastEq extends FastEq[MReg2SmsCode] {
    override def eqv(a: MReg2SmsCode, b: MReg2SmsCode): Boolean = {
      (a.smsCode    ===* b.smsCode) &&
      (a.submitReq  ===* b.submitReq)
    }
  }

  @inline implicit def univEq: UnivEq[MReg2SmsCode] = UnivEq.derive

  val smsCode   = GenLens[MReg2SmsCode](_.smsCode)
  val submitReq = GenLens[MReg2SmsCode](_.submitReq)

}


case class MReg2SmsCode(
                         smsCode      : Option[MSmsCodeS]     = None,
                         // Пересылка смс-кода - это наверное использовать поле с предыдущего шага.
                         submitReq    : Pot[MRegTokenResp]    = Pot.empty,
                       )
  extends ICanSubmit
  with EmptyProductPot
{

  override def canSubmit: Boolean = {
    smsCode.exists(_.typed.isValidNonEmpty) &&
    !submitReq.isPending
  }

}
