package io.suggest.ble.api.cordova.ble

import com.apple.ios.core.bluetooth.CBAdvData
import com.github.don.cordova.plugin.ble.central.BtDevice
import io.suggest.ble.api.cordova.ble.BtAdvData.ScanRecordToken
import io.suggest.ble.eddystone.{MEddyStoneUid, MFrameTypes}
import io.suggest.common.uuid.LowUuidUtil
import io.suggest.js.JsTypes
import io.suggest.msg.ErrorMsgs
import io.suggest.pick.JsBinaryUtil
import io.suggest.sjs.common.log.Log
import japgolly.univeq._

import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.16 11:34
  * Description: Поддержка парсинга маячков в EddyStone.
  */
object EddyStoneParser extends BeaconParserFactory {
  override type T = EddyStoneParser
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
case class EddyStoneParser(override val dev: BtDevice)
  extends BeaconParser
  with Log
{

  override type T = MEddyStoneUid

  /**
    * Парсинг eddystone-маячков.
    * @see Содрано с [[https://github.com/evothings/evothings-libraries/blob/master/libs/evothings/eddystone/eddystone.js#L79]]
    * @return Опциональный EddyStone или exception.
    */
  override def parse(): ParseRes_t = {
    val iter = for {
      // A device might be an Eddystone if it has advertisementData...
      advDataRaw <- dev.advertising.iterator
      if advDataRaw != null

      // Найти service data для eddystone.
      // Логика разветвляется, т.к. здесь многое зависит от платформы этого [мобильного] устройства.
      // Android зачем-то возвращает только сырой блобик с advertising data.
      // iOS возвращает разобранный объект с бинарями в некоторых местах.
      esAdBytes <- {
        val rawType = js.typeOf(advDataRaw.asInstanceOf[js.Any])

        if ( rawType ==* JsTypes.OBJECT ) {
          val eddyStoneUuidTok = ScanRecordToken.UuidToken.EDDY_STONE
          val resps = advDataRaw match {
            // Обнаружен сырой выхлоп из андройда
            case arrBuf: ArrayBuffer =>
              val parsedTokens = BtAdvData.parseScanRecord( new Uint8Array(arrBuf) )
              for {
                tok <- parsedTokens.iterator
                sd  <- tok match {
                  case sd: ScanRecordToken.ServiceData => sd :: Nil
                  case _ => Nil
                }
                if (sd.uuid matchUuid eddyStoneUuidTok)
              } yield {
                sd.data
              }

            // Обнаружен распарсенный выхлоп от apple-ios.
            case _ =>
              val iosAdData = advDataRaw.asInstanceOf[CBAdvData]
              for {
                sds <- iosAdData.kCBAdvDataServiceData.iterator
                (svcUuidPart, sd)  <- sds.iterator
                svcUuidTok = ScanRecordToken.UuidToken( svcUuidPart, BtAdvData.UuidHelper.B16 )
                if eddyStoneUuidTok matchUuid svcUuidTok
              } yield {
                new Uint8Array(sd)
              }
          }
          resps

        } else {
          // Что тут?
          LOG.warn( ErrorMsgs.JSON_PARSE_ERROR, msg = advDataRaw )
          Nil
        }
      }

      // Это eddystone какого-то типа. На руках есть service data, заявленная для eddystone uuid.
      // Надо разобрать байты, сверить тип с UID:
      frameCode = esAdBytes(0)
      if {
        // 2017.mar.29: Бывают инновационные маячки, которые излишне длинные фреймы, что не противоречит стандарту.
        // Могут и 20 байт прислать, где в хвосте 0x0000. Первый такой маячок был обнаружен в ТК Гулливер.
        // Парсим только первые N байт в UID-фреймах:
        frameCode ==* MFrameTypes.UID.frameCode &&
          esAdBytes.byteLength >= MFrameTypes.UID.frameMinByteLen
      }

    } yield {
      MEddyStoneUid(
        rssi    = dev.rssi.get,
        txPower = JsBinaryUtil.littleEndianToInt8(esAdBytes, 1),
        uid     = LowUuidUtil.hexStringToEddyUid(
          JsBinaryUtil.typedArrayToHexString(
            esAdBytes.subarray(2, MFrameTypes.UID.frameMinByteLen)
          )
        )
      )
    }

    // Вернуть первый найденный uid
    iter
      .toStream
      .headOption
      .map(Right.apply)
  }

  override def parserFailMsg = ErrorMsgs.CANT_PARSE_EDDY_STONE

}
