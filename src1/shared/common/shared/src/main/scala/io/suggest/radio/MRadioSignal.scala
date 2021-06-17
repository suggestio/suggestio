package io.suggest.radio

import io.suggest.ble.BeaconUtil
import io.suggest.scalaz.StringValidationNel
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scalaz.{Validation, ValidationNel}


object MRadioSignal {

  object Fields {
    final def TYPE          = "type"
    final def RSSI          = "rssi"
    final def FACTORY_UID   = "uid"
    final def CUSTOM_NAME   = "name"
    final def RSSI0         = "rssi0"
  }


  implicit def radioSignalJson: OFormat[MRadioSignal] = {
    val F = Fields
    (
      (__ \ F.TYPE).format[MRadioSignalType] and
      (__ \ F.RSSI).formatNullable[Int] and
      (__ \ F.FACTORY_UID).formatNullable[String] and
      (__ \ F.CUSTOM_NAME).formatNullable[String] and
      (__ \ F.RSSI0).formatNullable[Int]
    )(apply, unlift(unapply))
  }


  @inline implicit def univEq: UnivEq[MRadioSignal] = UnivEq.derive


  /** Validate beacon id. Expected lower-case string with EddyStone-UID format. */
  def validateEddyStoneNodeId(eddyId: String): ValidationNel[String, String] = {
    Validation.liftNel( eddyId )(
      !_.matches( BeaconUtil.EddyStone.EDDY_STONE_NODE_ID_RE_LC ),
      "e.eddy.stone.id.invalid"
    )
  }

}


/** Single radio signal description.
  *
  * @param typ Type of radio-signal.
  * @param rssi Current signal TX power in dBm.
  * @param rssi0 Known/declared RSSI at distance0m.
  * @param factoryUid Factory-defined universal (or pseudo-universal) identifier.
  *                   For BLE-beacons - EddyStone UID value.
  *                   For Wi-Fi - MAC-address (BSSID).
  * @param customName User-defined public name of radio-source.
  *                   For Wi-Fi - SSID (network name).
  */
final case class MRadioSignal(
                               typ                : MRadioSignalType,
                               rssi               : Option[Int],
                               factoryUid         : Option[String]      = None,
                               customName         : Option[String]      = None,
                               rssi0              : Option[Int]         = None,
                             )