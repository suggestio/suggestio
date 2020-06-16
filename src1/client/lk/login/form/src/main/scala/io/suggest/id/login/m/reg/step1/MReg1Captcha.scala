package io.suggest.id.login.m.reg.step1

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.EmptyProductPot
import io.suggest.id.reg.MRegTokenResp
import io.suggest.lk.m.captcha.MCaptchaS
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.06.19 18:23
  * Description: Состояние шага с вводом капчи.
  */
object MReg1Captcha {

  def empty = apply()

  implicit object MReg1CaptchaFastEq extends FastEq[MReg1Captcha] {
    override def eqv(a: MReg1Captcha, b: MReg1Captcha): Boolean = {
      (a ===* b) || (
        (a.captcha    ===* b.captcha) &&
        (a.submitReq  ===* b.submitReq)
      )
    }
  }

  @inline implicit def univEq: UnivEq[MReg1Captcha] = UnivEq.derive

  val captcha     = GenLens[MReg1Captcha](_.captcha)
  def submitReq   = GenLens[MReg1Captcha](_.submitReq)

}


/** Состояние капчи и запроса проверки исходной формы.
  *
  * @param captcha Состояние подформы ввода капчи.
  * @param submitReq Состояние запроса сабмита на сервер.
  */
case class MReg1Captcha(
                         captcha          : Option[MCaptchaS]          = None,
                         submitReq        : Pot[MRegTokenResp]    = Pot.empty,
                       )
  extends EmptyProductPot
{

  def canSubmit: Boolean = {
    !submitReq.isPending &&
    captcha.exists { c =>
      c.typed.isValidNonEmpty &&
      !c.contentReq.isPending
    }
  }

  /** Требуется ли инициализировать капчу путём отсылки сигнала CaptchaInit? */
  def isCaptchaNeedsInit: Boolean = {
    captcha.exists { c =>
      c.captchaImgUrlOpt.isEmpty &&
      c.contentReq.isEmpty &&
      !c.contentReq.isPending
    }
  }

}
