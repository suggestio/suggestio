package io.suggest.daemon

import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRW}
import io.suggest.sjs.dom2.DomQuick
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.04.2020 16:28
  * Description: Контроллер-для фонового sleep-таймера демона без cordova.
  *
  * TODO По идее, сам таймер должен запускаться вне выдачи, на стороне ServiceWorker'е или где-то ещё.
  *      Но т.к. webbluetooth-сканирования пока нет, то и пилить тут что-то сложное-заумное не требуется.
  */
class HtmlBgTimerAh[M](
                        dispatcher    : Dispatcher,
                        modelRW       : ModelRW[M, MHtmlBgTimerS],
                      )
  extends ActionHandler( modelRW )
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: DaemonSleepTimerSet =>
      val v0 = value

      m.options.fold[ActionResult[M]] {
        // Эффект отмены текущего таймера.
        v0.timerId.fold( noChange ) { timerId0 =>
          val fx = Effect.action {
            js.timers.clearInterval( timerId0 )
            DaemonSleepTimerUpdate( None )
          }
          effectOnly(fx)
        }

      } { opts =>
        // (Пере)выставить таймер.
        val setTimerFx = Effect.action {
          v0.timerId
            .foreach( js.timers.clearInterval )

          // TODO Отработать opts.everyBoot и opts.stopOnExit - возможно, это реализуется через таймер на стороне
          //      установленного ServiceWorker'а или что-то такое, и этот контроллер надо унести туда же.
          val onTimeA = opts.onTime
          val timerId = js.timers.setInterval( opts.every ) {
            dispatcher( onTimeA )
          }
          DaemonSleepTimerUpdate( Some( timerId ) )
        }
        effectOnly( setTimerFx )
      }


    // Обновление id таймера в состоянии.
    case m: DaemonSleepTimerUpdate =>
      val v0 = value

      if (m.timerId ==* v0.timerId) {
        noChange
      } else {
        val v2 = (MHtmlBgTimerS.timerId replace m.timerId)(v0)
        updatedSilent(v2)
      }

  }

}
