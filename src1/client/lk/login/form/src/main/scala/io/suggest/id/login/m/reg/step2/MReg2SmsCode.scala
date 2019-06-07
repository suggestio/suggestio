package io.suggest.id.login.m.reg.step2

import diode.FastEq
import diode.data.Pot
import io.suggest.id.login.m.reg.{ICanSubmit, IDataOpt}
import io.suggest.lk.m.sms.MSmsCodeS
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

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

}


case class MReg2SmsCode(
                         smsCode      : Option[MSmsCodeS]   = None,
                         // Пересылка смс-кода - это наверное использовать поле с предыдущего шага.
                         submitReq    : Pot[AnyRef]         = Pot.empty,
                       )
  extends ICanSubmit
  with IDataOpt[MSmsCodeS]
{

  override def canSubmit: Boolean = {
    smsCode.exists(_.typed.isValidNonEmpty) &&
    !submitReq.isPending
  }

  override def dataOpt = smsCode

}
