package io.suggest.sc.c.menu

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.sc.m.hdr.MenuOpenClose
import io.suggest.sc.m.menu.MMenuS
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.03.18 22:11
  * Description:
  */
class MenuAh[M](
                 modelRW: ModelRW[M, MMenuS]
               )
  extends ActionHandler(modelRW)
{

  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Экшен управления менюшкой.
    case m: MenuOpenClose =>
      val v0 = value
      if (v0.opened !=* m.open) {
        val v2 = v0.withOpened( m.open )
        updated( v2 )
      } else {
        noChange
      }

  }

}
