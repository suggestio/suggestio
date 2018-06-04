package io.suggest.sc.c.dev

import diode.{ActionHandler, ActionResult, Effect, ModelRW}
import io.suggest.dev.JsScreenUtil
import io.suggest.sc.m.dev.MScScreenS
import io.suggest.sc.m.grid.GridReConf
import io.suggest.sc.m.inx.ScCssReBuild
import io.suggest.sc.m.{ScreenReset, ScreenRszTimer}
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.controller.DomQuick

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.11.17 10:46
  * Description: Контроллер, слушающий события экрана устройства.
  */
class ScreenAh[M](
                   modelRW: ModelRW[M, MScScreenS]
                 )
  extends ActionHandler(modelRW)
{

  /** Кол-во миллисекунд срабатывания таймера задержки реакции на произошедший ресайз. */
  private def RSZ_TIMER_MS = 100


  override protected val handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал изменения размеров/ориентации экрана.
    case ScreenReset =>
      val v0 = value
      // TODO Проверять, изменился ли экран.
      v0.rszTimer.fold {
        val tp = DomQuick.timeoutPromise(RSZ_TIMER_MS)
        val fx = Effect {
          for (_ <- tp.fut) yield {
            ScreenRszTimer
          }
        }
        val v2 = v0.withRszTimer( Some( tp.timerId ) )
        updatedSilent(v2, fx)

      } { _ =>
        // Таймер уже запущен, просто обновить screen в состоянии свежим инстансом.
        noChange
      }


    // Сигнал срабатывания таймера отложенной реакции на изменение размеров экрана.
    case ScreenRszTimer =>
      // TODO Opt Проверять, изменился ли экран по факту? Может быть изменился и вернулся назад за время таймера?
      // Уведомить index-контроллер об изменении размера экрана
      val scCssRebuildFx = Effect.action( ScCssReBuild )

      // Уведомить контроллер плитки, что пора пересчитать плитку.
      val gridFx = Effect.action( GridReConf )

      // Забыть о сработавшем таймере.
      val screen2 = JsScreenUtil.getScreen()

      val v2 = value.copy(
        screen    = screen2,
        rszTimer  = None
      )

      val finalFx = scCssRebuildFx + gridFx
      updated(v2, finalFx)

  }

}
