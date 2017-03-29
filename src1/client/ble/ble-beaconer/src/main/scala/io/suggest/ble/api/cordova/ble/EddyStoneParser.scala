package io.suggest.ble.api.cordova.ble

import evothings.ble.DeviceInfo
import io.suggest.ble.beaconer.m.beacon.google.EddyStone
import io.suggest.common.radio.BleConstants
import io.suggest.common.uuid.LowUuidUtil
import io.suggest.sjs.common.bin.JsBinaryUtil
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.msg.ErrorMsgs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.16 11:34
  * Description: Поддержка парсинга маячков в EddyStone.
  */
object EddyStoneParser extends BeaconParserFactory {
  override type T = EddyStoneParser

  private val UID_PAYLOAD_BYTELEN = 18
}


/** Парсер для маячков по спеке Eddy Stone.
  *
  * @param dev BLE Device info.
  *
  * Инфа по EddyStone-UID-маячку от MS на Android выглядит так:
  * {{{
  *   {
  *     "address": "EF:3B:62:6A:2E:9B",
  *     "rssi": -60,
  *     "scanRecord": "AgEGAwOq/hUWqv4Aw6oRIjNEVWZ3iJkAAAAABFYAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
  *     "advertisementData":{
  *       "kCBAdvDataServiceUUIDs": ["0000feaa-0000-1000-8000-00805f9b34fb"],
  *       "kCBAdvDataServiceData": {
  *         "0000feaa-0000-1000-8000-00805f9b34fb": "AMOqESIzRFVmd4iZAAAAAARW"
  *       }
  *     }
  *   }
  * }}}
  *
  * А это сигнал EddyStone-URL маячка:
  * {{{
  *   {
  *     "address": "EF:3B:62:6A:2E:9B",
  *     "rssi": -67,
  *     "scanRecord": "AgEGAwOq/hAWqv4QwwBzdWdnZXN0LmlvAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
  *     "advertisementData": {
  *       "kCBAdvDataServiceUUIDs": ["0000feaa-0000-1000-8000-00805f9b34fb"],
  *       "kCBAdvDataServiceData": {
  *         "0000feaa-0000-1000-8000-00805f9b34fb": "EMMAc3VnZ2VzdC5pbw=="
  *       }
  *     }
  *   }
  * }}}
  *
  * TODO В теории, могут быть маячки, отсылающие пачкой несколько фреймов, а не по очереди.
  */
case class EddyStoneParser(override val dev: DeviceInfo)
  extends BeaconParser
  with Log
{

  override type T = EddyStone

  /**
    * Парсинг eddystone-маячков.
    * @see Содрано с [[https://github.com/evothings/evothings-libraries/blob/master/libs/evothings/eddystone/eddystone.js#L79]]
    * @return Опциональный EddyStone или exception.
    */
  override def parse(): ParseRes_t = {
    for {
      // A device might be an Eddystone if it has advertisementData...
      advData       <- dev.advertisementData.toOption

      // With serviceData...
      serviceData   <- advData.kCBAdvDataServiceData.toOption

      // And the 0xFEAA service.
      b64data       <- {
        val srvDataKey = BleConstants.Beacon.EddyStone.SERVICE_UUID_PREFIX_LC + BleConstants.SERVICES_BASE_UUID_LC
        serviceData.get( srvDataKey )
      }

    } yield {

      // Это EddyStone 100%. Загнать данные eddystone-сервиса в байтовый массив...
      val bytes = JsBinaryUtil.base64DecToArr(b64data)
      val frameCode = bytes(0)

      // 2017.mar.29: Бывают кривые маячки, которые шлют ошибочно длинные фреймы.
      // Могут и 20 байт обозначить, и 19. Первый такой маячок был обнаружен в ТК Гулливер.
      // Парсим первые N байт в UID-фреймах:
      if (frameCode == 0x00 && bytes.byteLength >= EddyStoneParser.UID_PAYLOAD_BYTELEN) {
        // Раз уж есть данные, то заодно уточнить значение RSSI.
        val rssi = dev.rssi.get

        val eddyStone = EddyStone(
          rssi    = rssi,
          txPower = JsBinaryUtil.littleEndianToInt8(bytes, 1),
          uid     = Some(
            LowUuidUtil.hexStringToEddyUid(
              JsBinaryUtil.typedArrayToHexString(
                bytes.subarray(2, EddyStoneParser.UID_PAYLOAD_BYTELEN)
              )
            )
          )
        )

        Right(eddyStone)

      } else {
        Left( frameCode.toString )
      }
    }
  }

  override def parserFailMsg = ErrorMsgs.CANT_PARSE_EDDY_STONE

}
