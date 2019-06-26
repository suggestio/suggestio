package io.suggest.id.login.m.reg.step3

import diode.FastEq
import io.suggest.id.login.m.reg.ICanSubmit
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 13:52
  * Description: Состояние чек-боксов и прочего содержимого на странице галочек.
  */
object MRegCheckBoxes {

  def empty = apply()

  implicit object MRegCheckBoxesFastEq extends FastEq[MRegCheckBoxes] {
    override def eqv(a: MRegCheckBoxes, b: MRegCheckBoxes): Boolean = {
      (a.tos            ===* b.tos) &&
      (a.pdn            ===* b.pdn)
    }
  }

  @inline implicit def univEq: UnivEq[MRegCheckBoxes] = UnivEq.derive

  val tos         = GenLens[MRegCheckBoxes]( _.tos )
  val pdn         = GenLens[MRegCheckBoxes]( _.pdn )

}


/** Контент под-формы финальных чекбоксов.
  *
  * @param tos Принял ли юзер условия соглашения сервиса?
  * @param pdn Принял ли юзер условия персональных данных?
  */
case class MRegCheckBoxes(
                           tos                 : MRegCheckBoxS           = MRegCheckBoxS.empty,
                           pdn                 : MRegCheckBoxS           = MRegCheckBoxS.empty,
                         )
  extends ICanSubmit
{

  def checkBoxes: List[MRegCheckBoxS] =
    tos :: pdn :: Nil

  override def canSubmit: Boolean = {
    (tos :: pdn :: Nil)
      .forall(_.isChecked)
  }

}
