package io.suggest.ble.api.cordova.ble

import evothings.EvothingsUtil
import evothings.ble.DeviceInfo
import io.suggest.ble.beaconer.m.beacon.google.EddyStone
import io.suggest.common.radio.BleConstants
import io.suggest.common.uuid.LowUuidUtil
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
}


/** Парсер для маячков по спеке Eddy Stone. */
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
  override def parse(): Option[T] = {
    for {
      // A device might be an Eddystone if it has advertisementData...
      advData       <- dev.advertisementData.toOption

      // With serviceData...
      serviceData   <- advData.kCBAdvDataServiceData.toOption

      // And the 0xFEAA service.
      b64data       <- serviceData.get( BleConstants.EDDY_STONE_SERVICE_UUID_PREFIX_LC + BleConstants.SERVICES_BASE_UUID_LC )

      // Загнать данные от eddystone-сервиса в байтовый массив...
      bytes = EvothingsUtil.base64DecToArr(b64data)
      if bytes(0) == 0x00 && bytes.byteLength < 18

      // Раз уж есть данные, то заодно уточнить значение RSSI.
      rssi          <- dev.rssi.toOption

    } yield {
      EddyStone(
        rssi    = rssi,
        txPower = EvothingsUtil.littleEndianToInt8(bytes, 1),
        uid     = Some(
          LowUuidUtil.hexStringToUuid(
            EvothingsUtil.typedArrayToHexString( bytes.subarray(2, 18) )
          )
        )
      )
    }
  }

  override def parserErrorMsg = ErrorMsgs.CANT_PARSE_EDDY_STONE

}
