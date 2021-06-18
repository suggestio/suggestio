package io.suggest.radio

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format


object MRadioSignalTypes extends StringEnum[MRadioSignalType] {

  /** Bluetooth radio signal. */
  case object BluetoothEddyStone extends MRadioSignalType("eddy") {
    override def goneAwayAfterSeconds = 5
    /** Measured signal power for EddyStone-UID transferred inside radio-signal. */
    override def rssi0 = None
    /** Measured distance for EddyStone is 0 cm. */
    override def distance0m = Some( 0 )
  }

  /** Wi-Fi radio signal. */
  case object WiFi extends MRadioSignalType("wifi") {
    override def goneAwayAfterSeconds = 20
    // Possible values from random measurments https://www.researchgate.net/figure/RSSI-versus-distance-for-BLE-Wi-Fi-and-XBee_fig5_317150846
    //
    override def rssi0 = Some( -27 )
    override def distance0m = Some( 0 )
  }


  override def values = findValues

}

sealed abstract class MRadioSignalType(override val value: String) extends StringEnumEntry {

  /** Guess signal as gone away and invisible after this duration (in seconds).
    * @return Duration in seconds.
    */
  def goneAwayAfterSeconds: Int

  /** Measured signal power at distance0m, in dBm. */
  def rssi0: Option[Int]

  /** Distance of rssi0 measurments. Usually - None and declared inside radioType.distance0m. */
  def distance0m: Option[Int]

}


object MRadioSignalType {

  @inline implicit def univEq: UnivEq[MRadioSignalType] = UnivEq.derive

  implicit def radioTypeJson: Format[MRadioSignalType] =
    EnumeratumUtil.valueEnumEntryFormat( MRadioSignalTypes )

}
