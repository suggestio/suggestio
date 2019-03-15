package io.suggest.id.login.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.id.login.m.{LoginShowHide, MLoginFormOverallS, SetForeignPc, SwitсhLoginTab}
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 18:14
  * Description: Контроллер формы логина в целом, т.е. крупными мазками.
  */
class FormAh[M](
                 modelRW: ModelRW[M, MLoginFormOverallS]
               )
  extends ActionHandler(modelRW)
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: LoginShowHide =>
      val v0 = value
      if (v0.isVisible ==* m.isShow) {
        noChange
      } else {
        val v2 = MLoginFormOverallS.isVisible.set(m.isShow)(v0)
        updated(v2)
      }


    case m: SwitсhLoginTab =>
      val v0 = value
      if (v0.loginTab ==* m.tab) {
        noChange
      } else {
        val v2 = MLoginFormOverallS.loginTab.set( m.tab )(v0)
        updated(v2)
      }


    case m: SetForeignPc =>
      val v0 = value
      if (v0.isForeignPc ==* m.isForeign) {
        noChange
      } else {
        val v2 = MLoginFormOverallS.isForeignPc.set( m.isForeign )( v0 )
        updated( v2 )
      }

  }

}
