package io.suggest.sc.c.dev

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.dev.JsScreenUtil
import io.suggest.sc.m.dev.MScScreenS
import io.suggest.sc.m.grid.GridReConf
import io.suggest.sc.m.{ScreenReset, ScreenRszTimer}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.DomQuick

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.11.17 10:46
  * Description: Контроллер, слушающий события экрана устройства.
  */
class ScreenAh[M](modelRW: ModelRW[M, MScScreenS]) extends ActionHandler(modelRW) {

  /** Кол-во миллисекунд срабатывания таймера задержки реакции на произошедший ресайз. */
  private def RSZ_TIMER_MS = 100


  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал изменения размеров/ориентации экрана.
    case ScreenReset =>
      val v0 = value
      val screen2 = JsScreenUtil.getScreen()
      v0.rszTimer.fold {
        val tp = DomQuick.timeoutPromise(RSZ_TIMER_MS)
        val fx = Effect {
          for (_ <- tp.fut) yield {
            ScreenRszTimer
          }
        }
        val v2 = v0.copy(
          screen   = screen2,
          rszTimer = Some( tp.timerId )
        )
        updated(v2, fx)

      } { _ =>
        // Таймер уже запущен, просто обновить screen в состоянии свежим инстансом.
        val v2 = v0.withScreen(
          screen = screen2
        )
        updated(v2)
      }


    // Сигнал срабатывания таймера отложенной реакции на изменение размеров экрана.
    case ScreenRszTimer =>
      // Уведомить контроллер плитки, что пора пересчитать плитку.
      val gridFx = Effect.action( GridReConf )

      // Забыть о сработавшем таймере.
      val v2 = value.withRszTimer(None)
      updated(v2, gridFx)

  }

}
