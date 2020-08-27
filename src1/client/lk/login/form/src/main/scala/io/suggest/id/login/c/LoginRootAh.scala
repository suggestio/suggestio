package io.suggest.id.login.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.id.login.m.epw.MEpwLoginS
import io.suggest.id.login.m.pwch.MPwNew
import io.suggest.id.login.m.{MLoginFormOverallS, MLoginRootS, PwVisibilityChange}
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.08.2020 15:03
  * Description: Корневой контроллер.
  */
class LoginRootAh[M](
                      modelRW: ModelRW[M, MLoginRootS],
                    )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: PwVisibilityChange =>
      val v0 = value

      val lens = if (m.isPwNew) {
        MLoginRootS.overall
          .composeLens( MLoginFormOverallS.pwNew )
          .composeLens( MPwNew.pwVisible )
      } else {
        MLoginRootS.epw
          .composeLens( MEpwLoginS.passwordVisible )
      }

      if (lens.get(v0) ==* m.visible) {
        noChange
      } else {
        val v2 = lens.set( m.visible )(v0)
        updated(v2)
      }

  }

}
