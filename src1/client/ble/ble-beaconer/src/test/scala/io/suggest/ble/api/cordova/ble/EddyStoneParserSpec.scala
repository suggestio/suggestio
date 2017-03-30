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
object EddyStoneParserSpec extends SimpleTestSuite {

  test("Parse some real 1st-gen MagicSystems EddyStone-UID beacon on Android") {

    val dev = js.Object().asInstanceOf[DeviceInfo]
    dev.address = "EF:3B:62:6A:2E:9B"
    dev.rssi = -60
    dev.scanRecord = "AgEGAwOq/hUWqv4Aw6oRIjNEVWZ3iJkAAAAABFYAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    dev.advertisementData = {
      val ad = js.Object().asInstanceOf[AdvertisementData]
      val svcUuid = "0000feaa-0000-1000-8000-00805f9b34fb"
      ad.kCBAdvDataServiceUUIDs = js.Array( svcUuid )
      ad.kCBAdvDataServiceData  = js.Dictionary[String](
        svcUuid -> "AMOqESIzRFVmd4iZAAAAAARW"
      )
      ad
    }

    val rEithOpt = EddyStoneParser(dev).parse()
    assert(rEithOpt.isDefined, rEithOpt.toString)

    val rEith = rEithOpt.get
    assert(rEith.isRight, rEith.toString)

    val r = rEith.right.get

    assertEquals(r.rssi, -60)
    //assertEquals(r.rssi0, -61)

    assertEquals( r.beaconUid, Some("aa112233445566778899-000000000456") )
  }


  test("Parse some strange eddystone-UID beacon from TK Gulliver (scan via iPhone)") {

    val dev = js.Object().asInstanceOf[DeviceInfo]
    dev.address = "35E90358-121B-4BE9-811A-0AE262D2FED5"
    dev.rssi = -95
    dev.advertisementData = {
      val ad = js.Object().asInstanceOf[AdvertisementData]
      val svcUuid = "0000feaa-0000-1000-8000-00805f9b34fb"
      ad.kCBAdvDataServiceUUIDs = js.Array(svcUuid)
      ad.kCBAdvDataServiceData = js.Dictionary[String](
        svcUuid -> "AOO6HFG6sxR+/ujlJSQjIiEgAAA="
      )
      ad
    }

    val rEithOpt = EddyStoneParser(dev).parse()
    assert( rEithOpt.isDefined, rEithOpt.toString )

    val rEith = rEithOpt.get
    assert( rEith.isRight, rEith.toString )

    val r = rEith.right.get

    assertEquals( r.txPower, -29 )
    assertEquals( r.beaconUid, Some("ba1c51bab3147efee8e5-252423222120") )
  }

}
