package io.suggest.id.login.m.reg

import diode.FastEq
import io.suggest.id.login.m.reg.step0.MReg0Creds
import io.suggest.id.login.m.reg.step1.MReg1Captcha
import io.suggest.id.login.m.reg.step2.MReg2SmsCode
import io.suggest.id.login.m.reg.step3.MReg3CheckBoxes
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.03.19 14:14
  * Description: Модель состояния регистрации по email-паролю.
  */
object MRegS {

  def empty = apply()

  implicit object MEpwRegSFastEq extends FastEq[MRegS] {
    override def eqv(a: MRegS, b: MRegS): Boolean = {
      (a.step            ===* b.step) &&
      (a.s0Creds         ===* b.s0Creds) &&
      (a.s1Captcha       ===* b.s1Captcha) &&
      (a.s2SmsCode       ===* b.s2SmsCode) &&
      (a.s3CheckBoxes    ===* b.s3CheckBoxes)
    }
  }

  @inline implicit def univEq: UnivEq[MRegS] = UnivEq.derive

  val step            = GenLens[MRegS](_.step)
  val s0Creds         = GenLens[MRegS](_.s0Creds)
  val s1Captcha       = GenLens[MRegS](_.s1Captcha)
  val s2SmsCode       = GenLens[MRegS](_.s2SmsCode)
  val s3CheckBoxes    = GenLens[MRegS](_.s3CheckBoxes)

}


/** Класс состояния формы регистрации по email-паролю.
  *
  * @param step Номер текущего шага регистрации.
  * @param s0Creds Нулевой шаг - ввода начальных реквизитов.
  * @param s1Captcha Состояние подформы ввода капчи.
  * @param s2SmsCode Состояние подформы ввода смс-кода.
  * @param s3CheckBoxes Состояние подформы чек-боксов.
  */
case class MRegS(
                  step           : MRegStep            = MRegSteps.values.head,
                  s0Creds        : MReg0Creds          = MReg0Creds.empty,
                  s1Captcha      : MReg1Captcha        = MReg1Captcha.empty,
                  s2SmsCode      : MReg2SmsCode        = MReg2SmsCode.empty,
                  s3CheckBoxes   : MReg3CheckBoxes     = MReg3CheckBoxes.empty,
                )
//extends ICanSubmit
{
  /*
    lazy val disableStep0: Boolean = {
      s1Captcha.exists(_.submitReq.isPending) ||
      s2SmsCode.nonEmpty ||
      s3CheckBoxes.nonEmpty
    }


    /** Активна ли кнопка сабмита. */
    override lazy val canSubmit: Boolean = {
      s0Creds.canSubmit &&
      (s1Captcha :: s2SmsCode :: s3CheckBoxes :: Nil)
        .iterator
        .forall( _.fold(true)(_.canSubmit) )
    }

  */

  def hasSubmitReqPending: Boolean = {
    s1Captcha.submitReq.isPending ||
    s2SmsCode.submitReq.isPending ||
    s3CheckBoxes.submitReq.isPending
  }

  lazy val hasSubmitReqPendingSome = Some( hasSubmitReqPending )

}


/** Интерфейс для метода canSubmit(). */
trait ICanSubmit {
  def canSubmit: Boolean
}
object ICanSubmit {
  implicit class IcsExtOps(val ics: ICanSubmit) extends AnyVal {
    def disableSubmit = !ics.canSubmit
  }
}

trait IDataOpt[A] {
  def dataOpt: Option[A]
}
