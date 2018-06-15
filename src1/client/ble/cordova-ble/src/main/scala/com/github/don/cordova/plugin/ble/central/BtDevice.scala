package com.github.don.cordova.plugin.ble.central

import com.apple.ios.core.bluetooth.CBAdvData

import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.06.18 15:38
  * Description: Interface of Device JSON.
  */
trait BtDevice extends js.Object {

  val name: js.UndefOr[String] = js.undefined

  /** Uniquely identifies the device.
    * The form of the address depends on the host platform:
    * Android -> MAC address, iOS -> UUID.
    */
  val id: String

  val rssi: js.UndefOr[Rssi_t] = js.undefined

  val advertising: js.UndefOr[ArrayBuffer | CBAdvData] = js.undefined

}
