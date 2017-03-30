package io.suggest.ble.beaconer.m.signals

import io.suggest.ble.BeaconSignal
import io.suggest.sjs.common.ble.MBleBeaconInfo
import io.suggest.sjs.common.fsm.{IFsmMsg, SjsFsm}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:09
  * Description: Сигналы для подписки на мониторинг маячков.
  */

/** Интерфейс сигналов, принмаемых beaconer fsm. */
trait IBeaconerInFsmMsg extends IFsmMsg


/** Сигнал подписки fsm на результаты работы beaconer'а. */
case class Subscribe( fsm: SjsFsm )
  extends IBeaconerInFsmMsg


/** Отказ от уведомлений о маячках. */
case class UnSubscribe( fsm: SjsFsm )
  extends IBeaconerInFsmMsg


/** Сигнал об обнаружении одного ble-маячка. */
case class BeaconDetected(
  beacon  : BeaconSignal,
  seen    : Long = System.currentTimeMillis()
)
  extends IBeaconerInFsmMsg


/** Исходящий сигнал о наблюдаемых маячках в текущий момент времени. */
case class BeaconsNearby(
  beacons: Seq[MBleBeaconInfo]
)
  extends IFsmMsg
{
  override def toString: String = {
    "BNear" + beacons.mkString("[", ",", "]")
  }
}

