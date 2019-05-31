package io.suggest.lk.m.captcha

import diode.FastEq
import diode.data.Pot
import io.suggest.lk.m.MTextFieldS
import io.suggest.text.StringUtil
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.03.19 14:48
  * Description: Состояние формы ввода капчи.
  */
object MCaptchaS {

  def empty = apply()

  implicit object MCaptchaSFastEq extends FastEq[MCaptchaS] {
    override def eqv(a: MCaptchaS, b: MCaptchaS): Boolean = {
      (a.typed     ===* b.typed) &&
      (a.reloading ===* b.reloading) &&
      (a.captchaId ===* b.captchaId)
    }
  }

  @inline implicit def univEq: UnivEq[MCaptchaS] = UnivEq.derive

  val captchaId   = GenLens[MCaptchaS]( _.captchaId )
  val typed       = GenLens[MCaptchaS]( _.typed )
  val reloading   = GenLens[MCaptchaS]( _.reloading )

  implicit class MCaptchaSExtOps( val cap: MCaptchaS ) extends AnyVal {
    def reset(captchaId: Option[String]): MCaptchaS = {
      cap.copy(
        captchaId = captchaId,
        typed = MTextFieldS.empty
      )
    }
  }

  def mkCaptchaId(): String =
    StringUtil.randomId( 5 )

  def defaultCaptcha = MCaptchaS( captchaId = Some(mkCaptchaId()) )

  def isTypedCapchaValid(typedCaptcha: String): Boolean = {
    typedCaptcha.nonEmpty && typedCaptcha.length < 20
  }
}


/** Контейнер данных js-капчи.
  *
  * @param captchaId Ссылка на картинку с капчей.
  * @param typed Введённый текст с картинки.
  */
case class MCaptchaS(
                      captchaId   : Option[String]  = None,
                      typed       : MTextFieldS     = MTextFieldS.empty,
                      reloading   : Pot[None.type]  = Pot.empty,
                    ) {

  def isShown: Boolean =
    captchaId.nonEmpty
  lazy val isShownSome: Some[Boolean] =
    Some( isShown )

}
