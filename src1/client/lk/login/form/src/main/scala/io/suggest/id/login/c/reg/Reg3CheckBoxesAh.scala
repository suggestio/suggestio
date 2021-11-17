package io.suggest.id.login.c.reg

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.id.login.m.reg.step3.{MReg3CheckBoxes, MRegCheckBoxS}
import io.suggest.id.login.m.Reg3CheckBoxChange
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

    // Update one checkbox state.
    case m: Reg3CheckBoxChange =>
      val v0 = value
      val cbState0 = v0.cbStates(m.checkBox)

      if ( cbState0.isChecked ==* m.isAccepted) {
        noChange
      } else {
        val cbState1 = (MRegCheckBoxS.isChecked replace m.isAccepted)(cbState0)
        val v2 = MReg3CheckBoxes.cbStates
          .modify { _ + (m.checkBox -> cbState1) }(v0)
        updated( v2 )
      }

  }

}
