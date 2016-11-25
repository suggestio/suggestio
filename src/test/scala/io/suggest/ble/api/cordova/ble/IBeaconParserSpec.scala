package io.suggest.ble.api.cordova.ble

import evothings.ble.{AdvertisementData, DeviceInfo}
import minitest._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.16 21:40
  * Description: Тесты для beacon-парсера [[IBeaconParser]].
  */
object IBeaconParserSpec extends SimpleTestSuite {

  test("Must successfully parse MagicSystems beacon #4689") {
    val dev = js.Object().asInstanceOf[DeviceInfo]
    dev.address = "DE:D1:AE:A5:5E:6B"
    dev.rssi = -63
    dev.scanRecord = "AgEEGv9MAAIVuUB/MPX4Rm6v+SVVa1f+bRI0ElHDAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    dev.advertisementData = {
      val ad = js.Object().asInstanceOf[AdvertisementData]
      ad.kCBAdvDataManufacturerData = "TAACFblAfzD1+EZur/klVWtX/m0SNBJRww=="
      ad
    }

    val rEithOpt = IBeaconParser(dev).parse()
    assert(rEithOpt.isDefined, rEithOpt.toString)

    val rEith = rEithOpt.get
    assert(rEith.isRight, rEith.toString)

    val r = rEith.right.get

    assertEquals(r.rssi, -63)
    assertEquals(r.rssi0, -61)

    assertEquals(r.major, 4660)
    assertEquals(r.minor, 4689)

    assertEquals(r.proximityUuid, "b9407f30-f5f8-466e-aff9-25556b57fe6d")
  }

}
