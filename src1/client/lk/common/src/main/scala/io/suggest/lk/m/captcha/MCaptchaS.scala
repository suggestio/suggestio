package io.suggest.lk.m.captcha

import diode.FastEq
import diode.data.Pot
import io.suggest.lk.m.input.MTextFieldS
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
      (a.contentReq      ===* b.contentReq) &&
      (a.captchaImgUrlOpt ===* b.captchaImgUrlOpt)
    }
  }

  @inline implicit def univEq: UnivEq[MCaptchaS] = UnivEq.derive

  def typed               = GenLens[MCaptchaS]( _.typed )
  def req                 = GenLens[MCaptchaS]( _.contentReq )
  def captchaImgUrlOpt    = GenLens[MCaptchaS]( _.captchaImgUrlOpt )

  def isTypedCapchaValid(typedCaptcha: String): Boolean = {
    typedCaptcha.nonEmpty && typedCaptcha.length < 10
  }

}


/** Контейнер данных js-капчи.
  *
  * @param contentReq Данные полученной с сервера капчи.
  * @param typed Введённый текст с картинки.
  * @param captchaImgUrlOpt Блобо-ссылка на картинку капчи.
  */
case class MCaptchaS(
                      typed               : MTextFieldS           = MTextFieldS.empty,
                      contentReq          : Pot[MCaptchaData]     = Pot.empty,
                      captchaImgUrlOpt    : Option[String]        = None,
                    )
