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
  * Created: 07.06.19 13:52
  * Description:
  */
object MRegCheckBoxes {

  implicit object MRegCheckBoxesFastEq extends FastEq[MRegCheckBoxes] {
    override def eqv(a: MRegCheckBoxes, b: MRegCheckBoxes): Boolean = {
      (a.tos            ===* b.tos) &&
      (a.pdn            ===* b.pdn) &&
      (a.acceptReq      ===* b.acceptReq)
    }
  }

  @inline implicit def univEq: UnivEq[MRegCheckBoxes] = UnivEq.derive

  val tos         = GenLens[MRegCheckBoxes]( _.tos )
  val pdn         = GenLens[MRegCheckBoxes]( _.pdn )
  val acceptReq   = GenLens[MRegCheckBoxes]( _.acceptReq )

}

/** Контент под-формы финальных чекбоксов.
  *
  * @param tos Принял ли юзер условия соглашения сервиса?
  * @param pdn Принял ли юзер условия персональных данных?
  * @param acceptReq Реквест к серверу с подтверждением регистрации.
  */
case class MRegCheckBoxes(
                           tos                 : MRegCheckBoxS           = MRegCheckBoxS.empty,
                           pdn                 : MRegCheckBoxS           = MRegCheckBoxS.empty,
                           acceptReq           : Pot[String]             = Pot.empty,
                         )
  extends ICanSubmit
{

  override def canSubmit: Boolean = {
    (tos :: pdn :: Nil)
      .forall(_.isChecked)
  }

}
