package io.suggest.cordova.background.timer

import io.suggest.sjs.common.async.AsyncUtil._
import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRW}
import io.suggest.daemon.DaemonSleepTimerSet
import cordova.plugins.background.timer._
import io.suggest.log.Log
import io.suggest.spa.DoNothing
import org.scalajs.dom

import scala.concurrent.Future
import scala.scalajs.js
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.04.2020 14:33
  * Description: Контроллер cordova-таймера фонового режима.
  */
object CordovaBgTimerAh {

  def hasCordovaBgTimer(): Boolean = {
    Try {
      !js.isUndefined( dom.window.BackgroundTimer )
    }
      .getOrElse( false )
  }

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
        val BT = dom.window.BackgroundTimer
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

  }

}
