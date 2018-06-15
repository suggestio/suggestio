package io.suggest.ble.api.cordova.ble

import com.apple.ios.core.bluetooth.CBAdvData
import com.github.don.cordova.plugin.ble.central.{BtDevice, Rssi_t}
import io.suggest.pick.JsBinaryUtil
import minitest._

import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer
import scala.scalajs.js.{Dictionary, UndefOr, |}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.16 21:40
  * Description: Тесты для beacon-парсера [[EddyStoneParser]].
  */
object EddyStoneParserSpec extends SimpleTestSuite {

  test("Parse some real 1st-gen MagicSystems EddyStone-UID beacon on Android") {
    // Имитируем bt-выхлоп андройда:
    val dev = new BtDevice {
      override val id = "EF:3B:62:6A:2E:9B"
      override val rssi: UndefOr[Rssi_t] = -60
      override val advertising: UndefOr[ArrayBuffer | CBAdvData] = {
        val scanRecord = "AgEGAwOq/hUWqv4Aw6oRIjNEVWZ3iJkAAAAABFYAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        JsBinaryUtil.base64DecToArr(scanRecord).buffer
      }
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

    val dev = new BtDevice {
      override val id = "35E90358-121B-4BE9-811A-0AE262D2FED5"
      override val rssi: UndefOr[Rssi_t] = -95
      override val advertising: UndefOr[ArrayBuffer | CBAdvData] = {
        val svcUuid = "0000feaa-0000-1000-8000-00805f9b34fb"
        new CBAdvData {
          override val kCBAdvDataServiceUUIDs: UndefOr[js.Array[String]] = {
            js.Array(svcUuid)
          }
          override val kCBAdvDataServiceData: UndefOr[Dictionary[ArrayBuffer]] = {
            js.Dictionary[ArrayBuffer](
              svcUuid -> JsBinaryUtil.base64DecToArr("AOO6HFG6sxR+/ujlJSQjIiEgAAA=").buffer
            )
          }
        }
      }
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
