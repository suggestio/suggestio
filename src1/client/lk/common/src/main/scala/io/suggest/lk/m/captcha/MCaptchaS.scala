package io.suggest.lk.m.captcha

import diode.FastEq
import diode.data.Pot
import io.suggest.lk.m.MTextFieldS
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
      (a.typed    ===* b.typed) &&
      (a.req      ===* b.req)
    }
  }

  @inline implicit def univEq: UnivEq[MCaptchaS] = UnivEq.derive

  val typed           = GenLens[MCaptchaS]( _.typed )
  val req             = GenLens[MCaptchaS]( _.req )

  def isTypedCapchaValid(typedCaptcha: String): Boolean = {
    typedCaptcha.nonEmpty && typedCaptcha.length < 10
  }

}


/** Контейнер данных js-капчи.
  *
  * @param req Данные полученной с сервера капчи.
  * @param typed Введённый текст с картинки.
  */
case class MCaptchaS(
                      typed           : MTextFieldS         = MTextFieldS.empty,
                      req             : Pot[MCaptchaData]   = Pot.empty,
                    ) {

  lazy val captchaImgUrlOpt = req.toOption.map(_.blobUrl)

}
