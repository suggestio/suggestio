package io.suggest.id.login.c

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.id.login.m.{EpwSetName, EpwSetPassword, MEpwLoginS}
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.03.19 16:13
  * Description: EmailPw-контроллер.
  */
class EpwAh[M](
                modelRW: ModelRW[M, MEpwLoginS],
              )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: EpwSetName =>
      val v0 = value
      if (v0.name ==* m.name) {
        noChange
      } else {
        val v2 = MEpwLoginS.name.set(m.name)(v0)
        updated(v2)
      }


    case m: EpwSetPassword =>
      val v0 = value
      if (v0.password ==* m.password) {
        noChange
      } else {
        val v2 = MEpwLoginS.password.set(m.password)(v0)
        updated(v2)
      }

  }

}
