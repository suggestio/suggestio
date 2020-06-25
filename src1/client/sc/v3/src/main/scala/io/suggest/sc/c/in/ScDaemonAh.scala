package io.suggest.sc.c.in

import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRO, ModelRW}
import io.suggest.sjs.common.async.AsyncUtil._
import io.suggest.sc.m.{ScDaemonSleepAlarm, ScDaemonDozed, ScDaemonFallSleepTimerSet, ScDaemonWorkProcess}
import io.suggest.sc.m.in.MScDaemon
import diode.Implicits._
import diode.data.Pot
import io.suggest.ble.beaconer.{BtOnOff, MBeaconerOpts}
import io.suggest.daemon.{DaemonBgModeSet, DaemonSleepTimerFinish, DaemonSleepTimerSet, MDaemonSleepTimer, MDaemonState, MDaemonStates}
import io.suggest.dev.{MOsFamilies, MOsFamily, MPlatformS}
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
                     platfromRO   : ModelRO[MPlatformS],
                     dispatcher   : Dispatcher,
                   )
  extends ActionHandler( modelRW )
  with Log
{ ah =>


  /** Использовать ли cordova-bgMode-плагин?
    * На iOS плагин не работает, и уже не компилируется.
    */
  def USE_BG_MODE: Boolean =
    platfromRO.value.osFamily contains[MOsFamily] MOsFamilies.Android

  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Запуск периодического фонового мониторинга.
    case m: ScDaemonDozed =>
      //logger.log( msg = m )

      val fx = Effect.action {
        DaemonSleepTimerSet(
          options = Option.when( m.isActive ) {
            MDaemonSleepTimer(
              every       = ScDaemonAh.DAEMON_ALARM_EVERY,
              onTime      = ScDaemonSleepAlarm( isActive = true ),
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
    case m: ScDaemonSleepAlarm =>
      //logger.log( msg = m )
      val v0 = value

      var fxsAcc = List.empty[Effect]

      val nextState = MDaemonStates.fromIsActive( m.isActive )
      if (!(v0.state contains[MDaemonState] nextState)) {
        fxsAcc ::= Effect.action {
          ScDaemonWorkProcess( isActive = m.isActive )
        }
      }

      if (USE_BG_MODE && (v0.cdvBgMode.isActive !=* m.isActive))
        fxsAcc ::= DaemonBgModeSet( isDaemon = m.isActive ).toEffectPure

      if (!m.isActive)
        fxsAcc ::= DaemonSleepTimerFinish.toEffectPure

      // Выставить pending в состояние демона.
      val v2 = MScDaemon.state.modify(_.pending())(v0)

      ah.updatedSilentMaybeEffect( v2, fxsAcc.mergeEffects )


    // Если true, значит запущен демон, активен WAKE_LOCK система какое-то время держит CPU включённым.
    // Запустить сканирование.
    case m: ScDaemonWorkProcess =>
      //logger.log( msg = m )

      val v0 = value
      val daemonState2 = MDaemonStates.fromIsActive( m.isActive )
      val v2 = (
        MScDaemon.state.modify( _.ready(daemonState2) ) andThen
        MScDaemon.fallSleepTimer.modify( _.pending() )
      )(v0)

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
            val a = if (USE_BG_MODE) ScDaemonSleepAlarm( isActive = false )
                    else ScDaemonWorkProcess( isActive = false )
            dispatcher( a )
          }
          ScDaemonFallSleepTimerSet( Some(timerId) )
        }

        // Выставить в состояние режим работы.
        updatedSilent( v2, fallSleepTimerFx + btScanFx )

      } else {
        // Завершение процесса демона. По идее, фоновое bt-сканирование завершилось само или завершится позже.
        var fxsAcc = List.empty[Effect]

        if (v0.state.isPending || (v0.state contains MDaemonStates.Work))
          fxsAcc ::= ScDaemonSleepAlarm( isActive = false ).toEffectPure

        for (timerId <- v0.fallSleepTimer) {
          fxsAcc ::= Effect.action {
            DomQuick.clearTimeout( timerId )
            ScDaemonFallSleepTimerSet( None )
          }
        }

        ah.updatedSilentMaybeEffect( v2, fxsAcc.mergeEffects )
      }


    // Сохранение выставленного таймера в состояние.
    case m: ScDaemonFallSleepTimerSet =>
      //logger.log( msg = m )
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
