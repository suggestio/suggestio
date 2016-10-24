package io.suggest.ble.beaconer.m.beacon.apple

import io.suggest.common.radio.{BeaconSignal, BleConstants}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:31
  * Description: Модель описания Apple iBeacon.
  */

case class IBeacon(
  override val rssi   : Int,
  override val rssi0  : Int,
  proximityUuid       : String,
  major               : Int,
  minor               : Int
)
  extends BeaconSignal
{

  override def distance0m: Int = 1

  override def uid: Option[String] = {
    val delim = BleConstants.Beacon.UID_DELIM
    val uidStr = proximityUuid + delim + major + delim + minor
    Some(uidStr)
  }

}
