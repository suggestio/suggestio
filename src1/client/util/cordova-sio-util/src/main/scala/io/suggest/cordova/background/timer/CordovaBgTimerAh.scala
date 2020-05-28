package io.suggest.cordova.background.timer

import io.suggest.sjs.common.async.AsyncUtil._
import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRW}
import io.suggest.daemon.{DaemonSleepTimerFinish, DaemonSleepTimerSet}
import cordova._
import cordova.plugins.background.timer.CordovaBackgroundTimerSettings
import io.suggest.log.Log
import io.suggest.sjs.JsApiUtil
import io.suggest.spa.DoNothing
import org.scalajs.dom

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.04.2020 14:33
  * Description: Контроллер cordova-таймера фонового режима.
  */
object CordovaBgTimerAh {

  private def CBGT = dom.window.BackgroundTimer

  def hasCordovaBgTimer(): Boolean =
    JsApiUtil.isDefinedSafe( CBGT )

}


class CordovaBgTimerAh[M](
                           dispatcher     : Dispatcher,
                           modelRW        : ModelRW[M, MCBgTimerS]
                         )
  extends ActionHandler( modelRW )
  with Log
{

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал к управлению таймером.
    case m: DaemonSleepTimerSet =>
      // Тут только эффект работы с плагином.
      val fx = Effect {
        val BT = CordovaBgTimerAh.CBGT
        for {
          _ <- m.options.fold [Future[Unit]] {
            BT.stopF()
          } { opts =>
            // Выставление/перезапись таймера.
            val onTimeA = opts.onTime
            BT.onTimerEventF( dispatcher(onTimeA) )
            BT.startF(
              new CordovaBackgroundTimerSettings {
                override val timerInterval      = opts.every.toMillis.toInt
                override val startOnBoot        = opts.everyBoot
                override val stopOnTerminate    = opts.stopOnExit
              }
            )
          }
        } yield {
          DoNothing
        }
      }

      effectOnly(fx)


    // Завершение работы - делать ничего не требуется.
    case DaemonSleepTimerFinish =>
      noChange

  }

}
