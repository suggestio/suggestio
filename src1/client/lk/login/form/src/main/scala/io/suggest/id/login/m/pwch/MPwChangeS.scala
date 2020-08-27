package io.suggest.id.login.m.pwch

import diode.FastEq
import diode.data.Pot
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

  def empty = apply()

  implicit object MPwChangeSFastEq extends FastEq[MPwChangeS] {
    override def eqv(a: MPwChangeS, b: MPwChangeS): Boolean = {
      (a.pwOld ===* b.pwOld) &&
      (a.pwOldVisible ==* b.pwOldVisible) &&
      (a.pwNew ===* b.pwNew) &&
      (a.submitReq ===* b.submitReq)
    }
  }

  def pwOld       = GenLens[MPwChangeS](_.pwOld)
  def pwOldVisible = GenLens[MPwChangeS](_.pwOldVisible)
  val pwNew       = GenLens[MPwChangeS](_.pwNew)
  def submitReq   = GenLens[MPwChangeS](_.submitReq)

  @inline implicit def univEq: UnivEq[MPwChangeS] = UnivEq.force

}


/** Состояние элементов формы смены пароля.
  *
  * @param pwOld Старый пароль.
  * @param pwNew Новый пароль и его подтверждение.
  * @param submitReq Состояние сабмита формы на сервер.
  */
case class MPwChangeS(
                       pwOld        : MTextFieldS             = MTextFieldS.empty,
                       pwOldVisible : Boolean                 = false,
                       pwNew        : MPwNew                  = MPwNew.empty,
                       submitReq    : Pot[None.type]          = Pot.empty,
                     ) {

  def canSubmit: Boolean = {
    !submitReq.isPending &&
    pwNew.canSubmit &&
    pwOld.isValidNonEmpty
  }

}
