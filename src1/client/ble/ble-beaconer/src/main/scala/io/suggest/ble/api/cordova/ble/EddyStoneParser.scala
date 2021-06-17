package io.suggest.ble.api.cordova.ble

import com.apple.ios.core.bluetooth.CBAdvData
import com.github.don.cordova.plugin.ble.central.BtDevice
import io.suggest.ble.api.cordova.ble.BtAdvData.ScanRecordToken
import io.suggest.ble.eddystone.MFrameTypes
import io.suggest.common.uuid.LowUuidUtil
import io.suggest.js.JsTypes
import io.suggest.msg.ErrorMsgs
import io.suggest.pick.JsBinaryUtil
import io.suggest.log.Log
import io.suggest.radio.{MRadioSignal, MRadioSignalTypes, RadioUtil}
import japgolly.univeq._

import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.10.16 11:34
  * Description: Beacon parser for EddyStone beacons.
  */
object EddyStoneParser extends BeaconParserFactory {
  override type T = EddyStoneParser
}


/** Parser for beacons according to EddyStone-UID specs.
  *
  * @param dev BLE Device info.
  *
  * Info about EddyStone-UID-beacon from MagicSystem on Android looks like that:
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
  * But, EddyStone-URL signal looks similar:
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
  * TODO Theoretically, may be EddyStone beacons, sending several frames at once.
  */
case class EddyStoneParser(override val dev: BtDevice)
  extends BeaconParser
  with Log
{

  override type T = MRadioSignal

  /**
    * Extract EddyStone frame from bluetooth adv. data.
    * @see From [[https://github.com/evothings/evothings-libraries/blob/master/libs/evothings/eddystone/eddystone.js#L79]]
    * @return Optional EddyStone data or exception.
    */
  override def parse(): ParseRes_t = {
    val s = (for {
      // A device might be an Eddystone if it has advertisementData...
      advDataRaw <- dev.advertising.iterator
      if advDataRaw != null

      // Find service data for eddystone.
      // Logic here splits, because logic is platform-depend (android, ios, etc).
      // Android returned raw blob with advertising data.
      // iOS returns slightly-parsed object with binaries inside some parts.
      esAdBytes: Uint8Array <- {
        val rawType = js.typeOf(advDataRaw.asInstanceOf[js.Any])

        if ( rawType ==* JsTypes.OBJECT ) {
          val eddyStoneUuidTok = ScanRecordToken.UuidToken.EDDY_STONE
          val resps = advDataRaw match {
            // Here is android raw blob.
            case arrBuf: ArrayBuffer =>
              val parsedTokens = BtAdvData.parseScanRecord( new Uint8Array(arrBuf) )
              for {
                tok <- parsedTokens.iterator
                sd  <- {
                  tok match {
                    case sd: ScanRecordToken.ServiceData => sd :: Nil
                    case _ => Nil
                  }
                }
                if {
                  val r = sd.uuid matchUuid eddyStoneUuidTok
                  r
                }
              } yield {
                sd.data
              }

            // Parsed output, apple-ios result.
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
          // Wtf?
          logger.warn( ErrorMsgs.JSON_PARSE_ERROR, msg = advDataRaw )
          Nil
        }
      }

    } yield {
      // This is EddyStone of some type. We have service data, possibly related to eddystone uuid.
      // Next, parse frame bytes, compare type with EddyStone-UID identifier:
      val frameCode = esAdBytes(0)

      if (
        // 2017-03-29: Exists over-innovate beacons, sending too big frames (ok to EddyStone standard).
        // Also, such beacons may send 20 bytes, and 0x0000 in the tail. First found in TK Gulliver.

        // Parse only firs N bytes only in EddyStone-UID frame:
        frameCode ==* MFrameTypes.UID.frameCode &&
          esAdBytes.byteLength >= MFrameTypes.UID.frameMinByteLen
      ) {
        val signal = MRadioSignal(
          rssi    = dev.rssi
            // Filter signal RSSI to valid value. Filter out dev.rssi=127, officially meaning None.
            .filter( RadioUtil.isRssiValid )
            .toOption,
          rssi0 = Some( JsBinaryUtil.littleEndianToInt8(esAdBytes, 1) ),
          factoryUid     = Some {
            LowUuidUtil.hexStringToEddyUid(
              JsBinaryUtil.typedArrayToHexString(
                esAdBytes.subarray(2, MFrameTypes.UID.frameMinByteLen)
              )
            )
          },
          typ = MRadioSignalTypes.BluetoothEddyStone,
        )

        Right( signal )

      } else {
        Left(frameCode)
      }
    })
      .to( LazyList )

    // Return first valid EddyStone-UID. If not found, return something, if any.
    s .find(_.isRight)
      .orElse( s.headOption )
  }

  override def parserFailMsg = ErrorMsgs.CANT_PARSE_EDDY_STONE

}
