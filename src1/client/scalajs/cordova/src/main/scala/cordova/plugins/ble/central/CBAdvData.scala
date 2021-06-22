package cordova.plugins.ble.central

import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.06.18 15:44
  * Description: Information extracted from a scanRecord. Some or all of the fields may
  * be undefined. This varies between BLE devices.
  * Depending on OS version and BLE device, additional fields, not documented
  * here, may be present.
  *
  * @see [[https://developer.apple.com/library/ios/documentation/CoreBluetooth/Reference/CBCentralManagerDelegate_Protocol/index.html#//apple_ref/doc/constant_group/Advertisement_Data_Retrieval_Keys]]
  */
trait CBAdvData extends js.Object {

  /**
    * The device's name. Might or might
    * not be equal to DeviceInfo.name. iOS caches DeviceInfo.name which means if
    * the name is changed on the device, the new name might not be visible.
    * kCBAdvDataLocalName is not cached and is therefore safer to use, when available.
    */
  val kCBAdvDataLocalName: js.UndefOr[String] = js.undefined

  /** Transmission power level as advertised by the device. */
  val kCBAdvDataTxPowerLevel: js.UndefOr[Int] = js.undefined

  /** A positive integer, the BLE channel
    * on which the device listens for connections. Ignore this number. */
  val kCBAdvDataChannel: js.UndefOr[Int] = js.undefined

  /** True if the device accepts  connections. False if it doesn't. */
  val kCBAdvDataIsConnectable: js.UndefOr[Boolean] = js.undefined

  /**
    * Array of strings, the UUIDs of services advertised by the device.
    * Formatted according to RFC 4122, all lowercase.
    */
  val kCBAdvDataServiceUUIDs: js.UndefOr[js.Array[String]] = js.undefined

  /**
    * Dictionary of strings to strings.
    * The keys are service UUIDs. The values are base-64-encoded binary data.
    */
  val kCBAdvDataServiceData: js.UndefOr[js.Dictionary[ArrayBuffer]] = js.undefined

  /**
    * Base-64-encoded binary data.
    * This field is used by BLE devices to advertise custom data that don't fit into
    * any of the other fields.
    */
  val kCBAdvDataManufacturerData: js.UndefOr[ArrayBuffer] = js.undefined

}
