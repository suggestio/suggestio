package io.suggest.sc.c.in

import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRO, ModelRW}
import io.suggest.sjs.common.async.AsyncUtil._
import io.suggest.sc.m.{DaemonSleepAlarm, ScDaemonDozed, ScDaemonFallSleepTimerSet, ScDaemonWorkProcess}
import io.suggest.sc.m.in.MScDaemon
import diode.Implicits._
import diode.data.Pot
import io.suggest.ble.beaconer.{BtOnOff, MBeaconerOpts, MBeaconerS}
import io.suggest.daemon.{DaemonSleepTimerSet, Daemonize, MDaemonSleepTimer, MDaemonStates}
import io.suggest.dev.MPlatformS
import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
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

  def DAEMON_ALARM_EVERY = 20.seconds // 5.minutes

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
      logger.log( msg = m )

      val fx = Effect.action {
        DaemonSleepTimerSet(
          options = Option.when( m.isActive ) {
            MDaemonSleepTimer(
              every       = ScDaemonAh.DAEMON_ALARM_EVERY,
              onTime      = DaemonSleepAlarm( isActive = true ),
              everyBoot   = true,
              stopOnExit  = true,
            )
          }
        )
      }

      val v0 = value
      val v2 = MScDaemon.state.modify { pot0 =>
        if (m.isActive) Pot.empty
        else pot0.ready( MDaemonStates.Sleep )
      }(v0)

      updatedSilent( v2, fx )


    // Срабатывание таймера запуска процесса демона.
    case m: DaemonSleepAlarm =>
      logger.log( msg = m )
      // TODO Проверить online-состояние через cordova MPlatformS, подписываясь на события online/offline.
      val fx = Daemonize( isDaemon = m.isActive ).toEffectPure

      // Выставить pending в состояние демона.
      val v0 = value
      val v2 = MScDaemon.state.modify(_.pending())(v0)

      updatedSilent(v2, fx)


    // Если true, значит запущен демон, активен WAKE_LOCK система какое-то время держит CPU включённым.
    // Запустить сканирование.
    case m: ScDaemonWorkProcess =>
      logger.log( msg = m )

      val v0 = value
      val daemonState2 = MDaemonStates.fromIsActive(m.isActive)
      val v2 = MScDaemon.state.modify( _.ready(daemonState2) )(v0)

      if (m.isActive) {
        // Провести короткое ble-сканирование. hardOff-флаг проверялся на стадии DaemonActivate, тут не проверяем.
        val btScanFx = Effect.action {
          BtOnOff(
            isEnabled = true,
            opts = MBeaconerOpts(
              hardOff        = false,
              askEnableBt    = false,
              oneShot        = true,
            )
          )
        }

        // На случай какой-либо задержки логики фонового сканирования, нужно гарантировать выключение демона через время.
        val fallSleepTimerFx = Effect.action {
          val timerId = DomQuick.setTimeout( ScDaemonAh.FALL_SLEEP_AFTER.toMillis.toInt ) { () =>
            dispatcher( DaemonSleepAlarm( isActive = false ) )
          }
          ScDaemonFallSleepTimerSet( Some(timerId) )
        }

        // Выставить в состояние режим работы.
        updatedSilent( v2, fallSleepTimerFx + btScanFx )

      } else {
        // Завершение процесса демона. По идее, фоновое bt-сканирование завершилось само или завершится позже.
        var fx: Effect = Effect.action( DaemonSleepAlarm(isActive = false) )

        for (timerId <- v0.fallSleepTimer) {
          fx = fx + Effect.action {
            DomQuick.clearTimeout( timerId )
            ScDaemonFallSleepTimerSet( None )
          }
        }

        updatedSilent(v2, fx)
      }


    // Сохранение выставленного таймера в состояние.
    case m: ScDaemonFallSleepTimerSet =>
      logger.log( msg = m )
      val v0 = value

      if (v0.fallSleepTimer.isPending) {
        val fxOpt = v0.fallSleepTimer
          .map(_clearFallSleepTimerSilentFx)
          .toOption

        val v2 = m.timerId.fold {
          MScDaemon.fallSleepTimer set Pot.empty[Int]
        } { timerId2 =>
          MScDaemon.fallSleepTimer.modify( _.ready(timerId2) )
        }(v0)

        ah.updatedSilentMaybeEffect( v2, fxOpt )

      } else {
        logger.warn( ErrorMsgs.INACTUAL_NOTIFICATION, msg = (m, v0.fallSleepTimer) )
        val fxOpt = m.timerId
          .map( _clearFallSleepTimerSilentFx )
        ah.maybeEffectOnly( fxOpt )
      }

  }


  /** Эффект очистки таймера без экшена. */
  private def _clearFallSleepTimerSilentFx(timerId: Int): Effect = {
    Effect.action {
      DomQuick.clearTimeout( timerId )
      DoNothing
    }
  }

}
