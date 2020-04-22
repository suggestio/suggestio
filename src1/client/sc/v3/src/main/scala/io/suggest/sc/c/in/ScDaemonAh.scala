package io.suggest.sc.c.in

import diode.{ActionHandler, ActionResult, Dispatcher, Effect, ModelRO, ModelRW}
import io.suggest.ble.beaconer.m.{BtOnOff, MBeaconerOpts, MBeaconerS}
import io.suggest.sjs.common.async.AsyncUtil._
import io.suggest.sc.m.{DaemonActivate, DaemonAlarm, DaemonAlarmSet}
import io.suggest.sc.m.in.MScDaemonInfo
import diode.Implicits._
import diode.data.Pot
import io.suggest.dev.MPlatformS
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.dom2.DomQuick
import io.suggest.spa.DiodeUtil.Implicits._
import io.suggest.spa.{DAction, DoNothing}

import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.2020 20:42
  * Description: Контроллер интеграции выдачи и подсистем для демонизации.
  */
object ScDaemonAh {

  def DAEMON_ALARM_EVERY = 5.minutes

  private def _clearIvlFx( alarmIvlId: Int, ts: Option[Long] ): Effect = {
    Effect.action {
      DomQuick.clearInterval( alarmIvlId )
      ts.fold[DAction]( DoNothing )( DaemonAlarmSet( None, _ ) )
    }
  }

}


class ScDaemonAh[M](
                     modelRW      : ModelRW[M, MScDaemonInfo],
                     beaconerRO   : ModelRO[MBeaconerS],
                     platfromRO   : ModelRO[MPlatformS],
                     dispatcher   : Dispatcher,
                   )
  extends ActionHandler( modelRW )
  with Log
{ ah =>

  private def _setIvlFx( ts: Long ): Effect = {
    Effect.action {
      val timerId = DomQuick.setInterval( ScDaemonAh.DAEMON_ALARM_EVERY.toMillis ) { () =>
        dispatcher( DaemonAlarm )
      }
      DaemonAlarmSet( Some(timerId), ts )
    }
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    case m: DaemonActivate =>
      println(m)

      val v0 = value
      var setPending = Option.empty[Long]
      var fxAcc = List.empty[Effect]

      if (m.isActive) {
        // Активация демона. TODO Надо запустить таймер BLE-сканирования.
        if (
          platfromRO.value.hasBle &&
          !beaconerRO.value.opts.hardOff &&
          v0.alarmId.isEmpty && !v0.alarmId.isPending
        ) {
          val ts = System.currentTimeMillis()
          setPending = Some(ts)
          // Если bluetooth, которые не выключен в настройках выдачи. Запустить таймер активации bluetooth-сканирования.
          fxAcc ::= _setIvlFx(ts)
        }

      } else {
        // Деактивация демона. TODO Остановить демонический таймер сканирования BLE.
        for (alarmIvlId <- v0.alarmId) {
          setPending = Some( System.currentTimeMillis() )
          fxAcc ::= ScDaemonAh._clearIvlFx( alarmIvlId, setPending )
        }
      }

      val v2Opt = for (ts <- setPending) yield {
        MScDaemonInfo.alarmId
          .modify( _.pending(ts) )(v0)
      }

      ah.optionalResult( v2Opt, fxAcc.mergeEffects )


    // Выставлен или сброшен "будильник" демона.
    case m: DaemonAlarmSet =>
      println(m)
      val v0 = value

      if (v0.alarmId isPendingWithStartTime m.timeStampMs) {
        val v2 = (m.timerId.fold {
          MScDaemonInfo.alarmId.set( Pot.empty )
        } { timerId =>
          MScDaemonInfo.alarmId.modify( _.ready(timerId) )
        })(v0)
        updatedSilent( v2 )

      } else {
        // Если таймер есть, а pending нет - отменить таймер.
        LOG.warn( ErrorMsgs.FSM_SIGNAL_UNEXPECTED, msg = (m, v0) )
        (for {
          timerId <- m.timerId
        } yield {
          val fx = ScDaemonAh._clearIvlFx( timerId, ts = None )
          effectOnly(fx)
        })
          .getOrElse( noChange )
      }


    // Срабатывание таймера демона.
    case DaemonAlarm =>
      println( DaemonAlarm )

      // Провести короткое ble-сканирование. hardOff-флаг проверялся на стадии DaemonActivate, тут не проверяем.
      // TODO Проверить online-состояние через cordova MPlatformS, подписываясь на события online/offline.
      val btFx = Effect.action {
        BtOnOff(
          isEnabled = true,
          opts = MBeaconerOpts(
            hardOff        = false,
            askEnableBt    = false,
            oneShot        = true,
          )
        )
      }

      effectOnly(btFx)

  }

}
