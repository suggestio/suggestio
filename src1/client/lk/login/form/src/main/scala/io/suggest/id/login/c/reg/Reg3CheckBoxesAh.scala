package io.suggest.id.login.c.reg

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.id.login.m.reg.step3.{MReg3CheckBoxes, MRegCheckBoxS, MRegCheckBoxes}
import io.suggest.id.login.m.{RegPdnSetAccepted, RegTosSetAccepted}
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.03.19 15:19
  * Description: Форма завершения регистрации.
  */
class Reg3CheckBoxesAh[M](
                           modelRW      : ModelRW[M, MReg3CheckBoxes]
                         )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал от галочкой правил пользования сервисом.
    case m: RegTosSetAccepted =>
      val v0 = value
      val cbs0 = v0.checkBoxes
      if (cbs0.tos.isChecked ==* m.isAccepted) {
        noChange

      } else {
        val v2 = MReg3CheckBoxes.checkBoxes
          .composeLens( MRegCheckBoxes.tos )
          .composeLens( MRegCheckBoxS.isChecked )
          .set(m.isAccepted)(v0)
        updated( v2 )
      }


    // Сигнал от галочки согласия на обработку персональных данных.
    case m: RegPdnSetAccepted =>
      val v0 = value

      if (v0.checkBoxes.pdn.isChecked ==* m.isAccepted) {
        noChange

      } else {
        val v2 = MReg3CheckBoxes.checkBoxes
          .composeLens( MRegCheckBoxes.pdn )
          .composeLens( MRegCheckBoxS.isChecked )
          .set( m.isAccepted )(v0)
        updated( v2 )
      }

  }

}
