package io.suggest.sc.c.inx

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.sc.m.SetDebug
import io.suggest.sc.sc3.MSc3Conf
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.07.2020 17:29
  * Description: Контроллер конфига.
  */
class ScConfAh[M](
                   modelRW: ModelRW[M, MSc3Conf],
                 )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: SetDebug =>
      val v0 = value
      if (v0.debug ==* m.isDebug) {
        noChange
      } else {
        val v2 = (MSc3Conf.debug set m.isDebug)(v0)
        updated(v2)
      }

  }

}
