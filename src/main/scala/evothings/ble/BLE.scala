package evothings.ble

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 12:33
  * Description: API for BLE.js.
  */
@JSName("evothings.ble")
@js.native
class BLE extends js.Object {

  def startScan(onDeviceFound : js.Function1[DeviceInfo, _],
                onScanError   : ErrorCallback_t,
                options       : ScanOptions = js.native): Unit = js.native

  def stopScan(): Unit = js.native

  def parseAdvertisementData(device: DeviceInfo): js.UndefOr[AdvertisementData] = js.native

  def getBondState(device   : IDeviceAddress,
                   success  : js.Function1[String, _],
                   fail     : ErrorCallback_t,
                   options  : IServiceUuids = js.native): Unit = js.native

  def getBondedDevices(success  : js.Function1[js.Array[DeviceInfo], _],
                       fail     : ErrorCallback_t,
                       options  : IServiceUuids): Unit = js.native

  def bond(deviceInfo : IDeviceAddress,
           success    : js.Function1[String, _],
           fail       : ErrorCallback_t): Unit = js.native

  def unbond(deviceInfo : IDeviceAddress,
             success    : js.Function1[String, _],
             fail       : ErrorCallback_t): Unit = js.native

  def connect(device  : IDeviceAddress,
              success : js.Function1[ConnectInfo, _],
              fail    : ErrorCallback_t): Unit = js.native

  def connectToDevice(device        : DeviceInfo,
                      connected     : js.Function1[DeviceInfo, _],
                      disconnected  : js.Function1[DeviceInfo, _],
                      fail          : ErrorCallback_t,
                      options       : ConnectOptions = js.native): Unit = js.native

  def close(deviceOrHandle: DeviceInfo | DevHandle_t): Unit = js.native

  def rssi(deviceOrHandle : DeviceInfo | DevHandle_t,
           success        : js.Function1[Rssi_t, _],
           fail           : ErrorCallback_t): Unit = js.native

  def services(deviceOrHandle : DeviceInfo | DevHandle_t,
               success        : js.Function1[js.Array[Service], _],
               fail           : ErrorCallback_t): Unit = js.native

  def characteristics(deviceOrHandle  : DeviceInfo | DevHandle_t,
                      serviceOrHandle : Service | ServiceHandle_t,
                      success         : js.Function1[js.Array[Characteristic], _],
                      fail            : ErrorCallback_t): Unit = js.native

  // TODO Descriptors

  // TODO charateristics notifications

  def testCharConversion(i: Int, success: js.Function1[js.Array[js.Any],_]): Unit = js.native

  def reset(success: js.Function0[_],
            fail: ErrorCallback_t): Unit = js.native

  def fromUtf8(arrayBuffer: ArrayBuffer): String = js.native

  def toUtf8(s: String): Uint8Array = js.native

  def getCanonicalUUID(uuid: String | Double): String = js.native

  // TODO readAllServiceData, readServiceData, getService, getCharacteristic, getDescriptor

  // TODO peripheral.*

}


@js.native
sealed trait Characteristic extends js.Object {

  var handle: CharacteristicHandle_t = js.native

  var uuid: String = js.native

  // TODO Enum needed
  var permission: js.Any = js.native

  // TODO Enum needed
  var properties: js.Any = js.native

  // TODO Enum needed
  var writeType: js.Any = js.native

}


@js.native
sealed trait ConnectOptions extends IServiceUuids {

  /** Set to false to disable automatic service discovery. Default is true. */
  var discoverServices: Boolean = js.native

}


@js.native
sealed trait Service extends js.Object {
  var handle: ServiceHandle_t = js.native
  var uuid: String = js.native
  var serviceType: String = js.native
}


@js.native
sealed trait ConnectInfo extends js.Object {

  var device: DeviceInfo = js.native

  var state: String = js.native

  var deviceHandle: DevHandle_t = js.native

}


@js.native
sealed trait IDeviceAddress extends js.Object {

  /** Uniquely identifies the device.
    * The form of the address depends on the host platform:
    * Android -> MAC address, iOS -> UUID.
    */
  var address: String = js.native

}


/** Info about a BLE device. */
@js.native
sealed trait DeviceInfo extends IDeviceAddress {

  /** The device's name, or nil (undefined?). */
  var name: js.UndefOr[String] = js.native

  /** A negative integer, the signal strength in decibels. */
  var rssi: js.UndefOr[Rssi_t] = js.native

  /** Base64-encoded binary data.
    * Its meaning is device-specific. Not available on iOS. */
  var scanRecord: js.UndefOr[String] = js.native

  /**
    * Object containing some of the data from the scanRecord.
    *
    * Available natively on iOS.
    *
    * Available on Android by parsing the scanRecord, which is implemented in the library EasyBLE:
    * @see [[https://github.com/evothings/evothings-libraries/blob/master/libs/evothings/easyble/easyble.js]]
    */
  var advertisementData: js.UndefOr[AdvertisementData] = js.native

}


/**
  * Information extracted from a scanRecord. Some or all of the fields may
  * be undefined. This varies between BLE devices.
  * Depending on OS version and BLE device, additional fields, not documented
  * here, may be present.
  */
@js.native
sealed trait AdvertisementData extends js.Object {

  /**
    * The device's name. Might or might
    * not be equal to DeviceInfo.name. iOS caches DeviceInfo.name which means if
    * the name is changed on the device, the new name might not be visible.
    * kCBAdvDataLocalName is not cached and is therefore safer to use, when available.
    */
  var kCBAdvDataLocalName: js.UndefOr[String] = js.native

  /** Transmission power level as advertised by the device. */
  var kCBAdvDataTxPowerLevel: js.UndefOr[Int] = js.native

  /** A positive integer, the BLE channel
    * on which the device listens for connections. Ignore this number. */
  var kCBAdvDataChannel: js.UndefOr[Int] = js.native

  /** True if the device accepts  connections. False if it doesn't. */
  var kCBAdvDataIsConnectable: js.UndefOr[Boolean] = js.native

  /**
    * Array of strings, the UUIDs of services advertised by the device.
    * Formatted according to RFC 4122, all lowercase.
    */
  var kCBAdvDataServiceUUIDs: js.UndefOr[js.Array[String]] = js.native

  /**
    * Dictionary of strings to strings.
    * The keys are service UUIDs. The values are base-64-encoded binary data.
    */
  var kCBAdvDataServiceData: js.UndefOr[js.Dictionary[String]] = js.native

  /**
    * Base-64-encoded binary data.
    * This field is used by BLE devices to advertise custom data that don't fit into
    * any of the other fields.
    */
  var kCBAdvDataManufacturerData: js.UndefOr[String] = js.native

}


@js.native
sealed trait IServiceUuids extends js.Object {

  var serviceUUIDs: js.Array[String] = js.native

}


@js.native
sealed trait ScanOptions extends IServiceUuids {

  var parseAdvertisementData: Boolean = js.native

}
