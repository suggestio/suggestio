package io.suggest.ble.api.cordova.ble

import evothings.EvothingsUtil
import evothings.ble.DeviceInfo
import io.suggest.ble.beaconer.m.beacon.apple.IBeacon
import io.suggest.common.uuid.LowUuidUtil
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.16 11:30
  * Description: Поддержка парсинга iBeacon.
  */
object IBeaconParser extends BeaconParserFactory {
  override type T = IBeaconParser
}

/**
  * Попытаться распарсить данные по iBeacon'у из инфы по девайсу.
  *
  * iBeacon Device info на андройде содержит -- это JSON в стиле:
  * {{{
  *  { "address": "C9:59:F6:28:82:CD",
  *    "rssi": -59,
  *    "scanRecord": "AgEEGv9MAAIVuUB/MPX4Rm6v+SVVa1f+bRI0EmXDAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
  *    "advertisementData":{
  *      "kCBAdvDataManufacturerData":"TAACFblAfzD1+EZur/klVWtX/m0SNBJlww=="
  *    }
  *  },
  *
  *  {"address": "DE:D1:AE:A5:5E:6B",
  *   "rssi": -63,
  *   "scanRecord": "AgEEGv9MAAIVuUB/MPX4Rm6v+SVVa1f+bRI0ElHDAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
  *   "advertisementData":{
  *     "kCBAdvDataManufacturerData":"TAACFblAfzD1+EZur/klVWtX/m0SNBJRww=="
  *   }
  *  }
  * }}}
  *
  * kCBAdvDataManufacturerData="TAACFblAfzD1+EZur/klVWtX/m0SNBJRww==" содержит байты в формате:
  * {{{
  *   0000000 | 004c 1502 40b9 307f f8f5 6e46 f9af 5525
  *   0000010 | 576b 6dfe 3412 5112 00c3
  * }}}
  * которые можно распарсить по схеме, похожей на эту:
  * {{{
  *   4C 00   # Company identifier code (0x004C == Apple)
  *   02      # Byte 0 of iBeacon advertisement indicator
  *   15      # Byte 1 of iBeacon advertisement indicator
  *   e2 c5 6d b5 df fb 48 d2 b0 60 d0 f5 a7 10 96 e0   # iBeacon proximity uuid
  *   00 00   # major
  *   00 00   # minor
  *   c5      # The 2's complement of the calibrated Tx Power
  * }}}
  *
  * @param dev ble-девайс.
  * @return None, если это не iBeacon или если какой-то кривой iBeacon.
  * @see [[http://stackoverflow.com/a/19040616]]
  */
case class IBeaconParser(override val dev: DeviceInfo)
  extends BeaconParser
  with Log
{

  override type T = IBeacon

  override def parserErrorMsg = ErrorMsgs.CANT_PARSE_IBEACON

  def parse(): Option[IBeacon] = {
    // Option тут не требуется, поэтому делаем for{} в понятиях UndefOr[] вместо Option.
    val ibeaconUnd = for {
      // Обязательно должна быть advertisementData...
      advData         <- dev.advertisementData

      // Ябблочный протокол не стандартен, поэтому всё падает в manuf.data
      manufDataB64    <- advData.kCBAdvDataManufacturerData

      // Десериализация из base64 в байтовый массив.
      bytes = EvothingsUtil.base64DecToArr(manufDataB64)

      // Проверить содержимое, там вначале обычно 4 стабильных байта: 4c 00 02 15
      if bytes(0) == 0x4c && bytes(1) == 0x00 &&    // if apple and...
         bytes(2) == 0x02 && bytes(3) == 0x15       // ... and if iBeacon adv indicated

      rssi <- dev.rssi

    } yield {
      IBeacon(
        rssi          = rssi,
        // TODO отформатировать в UUID с помощью дефисов.
        proximityUuid = LowUuidUtil.hexStringToUuid(
          EvothingsUtil.typedArrayToHexString(
            bytes.subarray(4, 20)
          )
        ),
        major         = EvothingsUtil.bigEndianToUint16(bytes, 20),
        minor         = EvothingsUtil.bigEndianToUint16(bytes, 22),
        rssi0         = EvothingsUtil.littleEndianToInt8(bytes, 24)
      )
    }

    // Вернуть Option[] вместо undefined.
    ibeaconUnd.toOption
  }

}
