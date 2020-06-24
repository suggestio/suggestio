package io.suggest.sc.c.dia

import diode.{ActionHandler, ActionResult, ModelRW}
import io.suggest.sc.m.{ResetUrlRoute, SettingsDiaOpen}
import io.suggest.sc.m.dia.settings.MScSettingsDia
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.2020 18:59
  * Description: Контроллер диалога настроек выдачи.
  */
class ScSettingsDiaAh[M](
                          modelRW    : ModelRW[M, MScSettingsDia],
                        )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: SettingsDiaOpen =>
      val v0 = value

      if (v0.opened ==* m.opened) {
        noChange

      } else {
        val v2 = (MScSettingsDia.opened set m.opened)(v0)
        val fx = ResetUrlRoute.toEffectPure
        updated(v2, fx)
      }

  }

}
