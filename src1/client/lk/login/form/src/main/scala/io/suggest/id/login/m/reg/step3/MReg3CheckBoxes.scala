package io.suggest.id.login.m.reg.step3

import diode.FastEq
import diode.data.Pot
import io.suggest.id.login.m.reg.ICanSubmit
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.19 17:42
  * Description: Состояние под-формы окончания регистрации.
  * Когда юзер возвращается из гос.услуг первый раз - надо задать пару вопросов.
  */
object MReg3CheckBoxes {

  def empty = apply()

  implicit object MReg3CheckBoxesFastEq extends FastEq[MReg3CheckBoxes] {
    override def eqv(a: MReg3CheckBoxes, b: MReg3CheckBoxes): Boolean = {
      (a.checkBoxes     ===* b.checkBoxes) &&
      (a.submitReq      ===* b.submitReq)
    }
  }

  @inline implicit def univEq: UnivEq[MReg3CheckBoxes] = UnivEq.derive

  val checkBoxes  = GenLens[MReg3CheckBoxes]( _.checkBoxes )
  val submitReq   = GenLens[MReg3CheckBoxes]( _.submitReq )

}


/** Контейнер данных состояния окончания регистрации.
  *
  * @param checkBoxes Состояние формы чекбоксов, если она есть.
  * @param submitReq Реквест сабмита этого шага на сервер.
  */
case class MReg3CheckBoxes(
                            checkBoxes          : MRegCheckBoxes          = MRegCheckBoxes.empty,
                            submitReq           : Pot[AnyRef]             = Pot.empty,
                          )
  extends ICanSubmit
{

  override def canSubmit: Boolean = {
    //checkBoxes.exists(_.canSubmit)
    checkBoxes.canSubmit &&
    !submitReq.isPending
  }

}


