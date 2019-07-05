package io.suggest.id.login.m.reg.step3

import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 13:52
  * Description: Состояние чек-боксов и прочего содержимого на странице галочек.
  */
object MReg3CheckBoxes {

  def empty = apply()

  implicit object MReg3CheckBoxesFastEq extends FastEq[MReg3CheckBoxes] {
    override def eqv(a: MReg3CheckBoxes, b: MReg3CheckBoxes): Boolean = {
      (a.tos            ===* b.tos) &&
      (a.pdn            ===* b.pdn)
    }
  }

  @inline implicit def univEq: UnivEq[MReg3CheckBoxes] = UnivEq.derive

  val tos         = GenLens[MReg3CheckBoxes]( _.tos )
  val pdn         = GenLens[MReg3CheckBoxes]( _.pdn )

}


/** Контент под-формы финальных чекбоксов.
  *
  * @param tos Принял ли юзер условия соглашения сервиса?
  * @param pdn Принял ли юзер условия персональных данных?
  */
case class MReg3CheckBoxes(
                            tos                 : MRegCheckBoxS           = MRegCheckBoxS.empty,
                            pdn                 : MRegCheckBoxS           = MRegCheckBoxS.empty,
                          ) {

  def checkBoxes: List[MRegCheckBoxS] =
    tos :: pdn :: Nil

  def canSubmit: Boolean = {
    (tos :: pdn :: Nil)
      .forall(_.isChecked)
  }

}
