package io.suggest.ble.ibeacon

import io.suggest.ble.{BeaconSignal, BleConstants}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:31
  * Description: Модель описания Apple iBeacon.
  */

case class MiBeacon(
  override val rssi   : Int,
  override val rssi0  : Int,
  proximityUuid       : String,
  major               : Int,
  minor               : Int
)
  extends BeaconSignal
{

  override def distance0m: Int = 1

  override def beaconUid: Option[String] = {
    val delim = BleConstants.Beacon.UID_DELIM
    val uidStr = proximityUuid + delim + major + delim + minor
    Some(uidStr)
  }

}
