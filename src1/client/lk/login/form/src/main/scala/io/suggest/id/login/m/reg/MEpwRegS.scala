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
      (a.submitReq    ===* b.submitReq) &&
      (a.phone        ===* b.phone)
    }
  }

  @inline implicit def univEq: UnivEq[MEpwRegS] = UnivEq.derive

  val email       = GenLens[MEpwRegS]( _.email )
  val captcha     = GenLens[MEpwRegS]( _.captcha )
  val submitReq   = GenLens[MEpwRegS]( _.submitReq )
  val phone       = GenLens[MEpwRegS]( _.phone )

}


/** Класс состояния формы регистрации по email-паролю.
  *
  * @param email Адрес электронной почты.
  * @param captcha Состояние капчи: инициализированна, введена, ошибка - всё через Pot.
  * @param phone Поле ввода номера телефона.
  */
case class MEpwRegS(
                     email            : MTextFieldS         = MTextFieldS.empty,
                     phone            : MTextFieldS         = MTextFieldS.empty,
                     captcha          : Option[MCaptchaS]   = None,
                     // TODO Сюда добавить ввод смс-кода опциональный, и добавить состояние для формы галочек соглашений.
                     submitReq        : Pot[String]         = Pot.empty,
                   ) {

  lazy val isSubmitReqPendingSome: Some[Boolean] = Some( submitReq.isPending )

}
