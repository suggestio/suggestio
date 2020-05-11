package io.suggest.daemon

import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRW}
import io.suggest.sjs.dom2.DomQuick
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import japgolly.univeq._

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
            DomQuick.clearInterval( timerId0 )
            DaemonSleepTimerUpdate( None )
          }
          effectOnly(fx)
        }

      } { opts =>
        // (Пере)выставить таймер.
        val setTimerFx = Effect.action {
          v0.timerId
            .foreach( DomQuick.clearInterval )

          // TODO Отработать opts.everyBoot и opts.stopOnExit - возможно, это реализуется через таймер на стороне
          //      установленного ServiceWorker'а или что-то такое, и этот контроллер надо унести туда же.
          val onTimeA = opts.onTime
          val timerId = DomQuick.setInterval( opts.every.toMillis.toInt ) { () =>
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
        val v2 = (MHtmlBgTimerS.timerId set m.timerId)(v0)
        updatedSilent(v2)
      }

  }

}
