package io.suggest.sc.c.in

import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRO, ModelRW}
import io.suggest.sjs.common.async.AsyncUtil._
import io.suggest.sc.m.{DaemonSleepAlarm, ScDaemonDozed, ScDaemonFallSleepTimerSet, ScDaemonWorkProcess}
import io.suggest.sc.m.in.MScDaemon
import diode.Implicits._
import io.suggest.ble.beaconer.{BtOnOff, MBeaconerOpts, MBeaconerS}
import io.suggest.daemon.{DaemonSleepTimerSet, Daemonize, MDaemonSleepTimer}
import io.suggest.dev.MPlatformS
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.dom2.DomQuick
import japgolly.univeq._
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.DoNothing

import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.2020 20:42
  * Description: Контроллер интеграции выдачи и подсистем для демонизации.
  */
object ScDaemonAh {

  def DAEMON_ALARM_EVERY = 5.minutes

  /** Защитный таймер для гарантированного выключения при задержке работы. */
  def FALL_SLEEP_AFTER = 7.seconds

}


/** Контроллер для экшенов демона. */
class ScDaemonAh[M](
                     modelRW      : ModelRW[M, MScDaemon],
                     beaconerRO   : ModelRO[MBeaconerS],
                     platfromRO   : ModelRO[MPlatformS],
                     dispatcher   : Dispatcher,
                   )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Запуск периодического фонового мониторинга.
    case m: ScDaemonDozed =>
      println(m)

      val fx = Effect.action {
        DaemonSleepTimerSet(
          options = Option.when( m.isActive ) {
            MDaemonSleepTimer(
              every       = ScDaemonAh.DAEMON_ALARM_EVERY,
              onTime      = DaemonSleepAlarm,
              everyBoot   = true,
              stopOnExit  = true,
            )
          }
        )
      }

      effectOnly( fx )


    // Срабатывание таймера запуска процесса демона.
    case DaemonSleepAlarm =>
      println( DaemonSleepAlarm )

      // TODO Проверить online-состояние через cordova MPlatformS, подписываясь на события online/offline.
      val fx = Daemonize( isDaemon = true ).toEffectPure
      effectOnly(fx)


    // Если true, значит запущен демон, активен WAKE_LOCK система какое-то время держит CPU включённым.
    // Запустить сканирование.
    case m: ScDaemonWorkProcess =>
      if (m.isActive) {
        // Провести короткое ble-сканирование. hardOff-флаг проверялся на стадии DaemonActivate, тут не проверяем.
        val btScanFx = Effect.action {
          BtOnOff(
            isEnabled = true,
            opts = MBeaconerOpts(
              hardOff        = false,
              askEnableBt    = false,
              oneShot        = true,
              // TODO !!! XXX Пробросить в onChange = фунцию перезагрузки плитки с Daemonize(isDaemon = false) после завершения обработки возможного запроса на сервер.
            )
          )
        }

        // На случай какой-либо задержки логики фонового сканирования, нужно гарантировать выключение демона через время.
        val fallSleepTimerFx = Effect.action {
          val timerId = DomQuick.setTimeout( ScDaemonAh.FALL_SLEEP_AFTER.toMillis ) { () =>
            dispatcher( Daemonize(false) )
          }
          ScDaemonFallSleepTimerSet( Some(timerId) )
        }

        effectOnly( fallSleepTimerFx + btScanFx )

      } else {
        // Раздемонизация. По идее, фоновое bt-сканирование завершилось само,
        // и теперь параллельно уже запускается активное непрерывное bt-сканирование. Просто не вмешиваемся.
        value.fallSleepTimer.fold( noChange ) { timerId =>
          val fx = Effect.action {
            DomQuick.clearTimeout( timerId )
            ScDaemonFallSleepTimerSet( None )
          }
          effectOnly( fx )
        }
      }


    // Выставление таймера
    case m: ScDaemonFallSleepTimerSet =>
      val v0 = value

      if (v0.fallSleepTimer ==* m.timerId) {
        noChange
      } else {
        val v2 = (MScDaemon.fallSleepTimer set m.timerId)(v0)
        val fxOpt = for (timerId <- v0.fallSleepTimer) yield Effect.action {
          DomQuick.clearTimeout( timerId )
          DoNothing
        }
        this.updatedSilentMaybeEffect(v2, fxOpt)
      }

  }

}
