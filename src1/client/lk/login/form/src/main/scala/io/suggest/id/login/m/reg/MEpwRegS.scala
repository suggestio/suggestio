package io.suggest.id.login.m.reg

import diode.FastEq
import diode.data.Pot
import io.suggest.lk.m.MTextFieldS
import io.suggest.lk.m.captcha.MCaptchaS
import monocle.macros.GenLens
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.03.19 14:14
  * Description: Модель состояния регистрации по email-паролю.
  */
object MEpwRegS {

  implicit object MEpwRegSFastEq extends FastEq[MEpwRegS] {
    override def eqv(a: MEpwRegS, b: MEpwRegS): Boolean = {
      (a.email        ===* b.email) &&
      (a.captcha      ===* b.captcha) &&
      (a.submitReq    ===* b.submitReq)
    }
  }

  @inline implicit def univEq: UnivEq[MEpwRegS] = UnivEq.derive

  val email       = GenLens[MEpwRegS]( _.email )
  val captcha     = GenLens[MEpwRegS]( _.captcha )
  val submitReq   = GenLens[MEpwRegS]( _.submitReq )

}


/** Класс состояния формы регистрации по email-паролю.
  *
  * @param email Адрес электронной почты.
  * @param captcha Состояние капчи: инициализированна, введена, ошибка - всё через Pot.
  */
case class MEpwRegS(
                     // TODO Номер телефона и sms-код.
                     email            : MTextFieldS         = MTextFieldS.empty,
                     captcha          : MCaptchaS           = MCaptchaS.defaultCaptcha,
                     submitReq        : Pot[String]         = Pot.empty,
                   )
