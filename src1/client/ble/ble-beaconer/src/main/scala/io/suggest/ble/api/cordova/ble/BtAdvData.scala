package io.suggest.ble.api.cordova.ble

import io.suggest.ble.BleConstants
import io.suggest.pick.JsBinaryUtil
import japgolly.univeq._

import scala.annotation.tailrec
import scala.scalajs.js.typedarray.Uint8Array

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.06.18 15:18
  * Description: Utils for parsing bluetooth adv data.
  */
object BtAdvData {

  sealed trait UuidHelper {
    def to16lc(part: String): String
  }
  object UuidHelper {

    case object B2 extends UuidHelper {
      override def to16lc(part: String): String =
        BleConstants.FOUR_ZEROES + part + BleConstants.SERVICES_BASE_UUID_LC
    }

    case object B4 extends UuidHelper {
      override def to16lc(part: String): String =
        part + BleConstants.SERVICES_BASE_UUID_LC
    }

    case object B16 extends UuidHelper {
      override def to16lc(part: String): String =
        part
    }

  }


  /** Parsed token interface. */
  sealed trait ScanRecordToken

  object ScanRecordToken {

    final case class UuidToken(part: String, helper: UuidHelper) extends ScanRecordToken {
      lazy val uuid16lc = helper.to16lc(part)

      def matchUuid(serviceUuid: UuidToken): Boolean = {
        // Compare parts of service UUID, only if parts lengths matches.
        if (part.length ==* serviceUuid.part.length) {
          // Same length of parts: compare these parts. Usually, 2 bytes here for 0xFEAA for EddyStone.
          part equalsIgnoreCase serviceUuid.part
        } else {
          // Different len for parts. Compare full Service UUIDs
          uuid16lc equalsIgnoreCase serviceUuid.uuid16lc
        }
      }

    }
    object UuidToken {
      /** UUID-part for EddyStone service UUID. */
      def EDDY_STONE = apply( BleConstants.Beacon.EddyStone.SERVICE_UUID_16B_LC, UuidHelper.B2 )
    }

    /** Device local name. */
    final case class LocalName(name: String) extends ScanRecordToken

    /** Normal device signal tx-power. */
    final case class TxPower(txPowerDb: Int) extends ScanRecordToken

    final case class ServiceData(uuid: UuidToken, data: Uint8Array) extends ScanRecordToken

    final case class ManufacturerData(data: Uint8Array) extends ScanRecordToken

  }


  object GattTypes {
    final val EBLE_FLAGS           = 0x01;//«Flags»	Bluetooth Core Specification:
    final val EBLE_16BitUUIDInc    = 0x02;//«Incomplete List of 16-bit Service Class UUIDs»	Bluetooth Core Specification:
    final val EBLE_16BitUUIDCom    = 0x03;//«Complete List of 16-bit Service Class UUIDs»	Bluetooth Core Specification:
    final val EBLE_32BitUUIDInc    = 0x04;//«Incomplete List of 32-bit Service Class UUIDs»	Bluetooth Core Specification:
    final val EBLE_32BitUUIDCom    = 0x05;//«Complete List of 32-bit Service Class UUIDs»	Bluetooth Core Specification:
    final val EBLE_128BitUUIDInc   = 0x06;//«Incomplete List of 128-bit Service Class UUIDs»	Bluetooth Core Specification:
    final val EBLE_128BitUUIDCom   = 0x07;//«Complete List of 128-bit Service Class UUIDs»	Bluetooth Core Specification:
    final val EBLE_SHORTNAME       = 0x08;//«Shortened Local Name»	Bluetooth Core Specification:
    final val EBLE_LOCALNAME       = 0x09;//«Complete Local Name»	Bluetooth Core Specification:
    final val EBLE_TXPOWERLEVEL    = 0x0A;//«Tx Power Level»	Bluetooth Core Specification:
    final val EBLE_DEVICECLASS     = 0x0D;//«Class of Device»	Bluetooth Core Specification:
    final val EBLE_SIMPLEPAIRHASH  = 0x0E;//«Simple Pairing Hash C»	Bluetooth Core Specification:​«Simple Pairing Hash C-192»	​Core Specification Supplement, Part A, section 1.6
    final val EBLE_SIMPLEPAIRRAND  = 0x0F;//«Simple Pairing Randomizer R»	Bluetooth Core Specification:​«Simple Pairing Randomizer R-192»	​Core Specification Supplement, Part A, section 1.6
    final val EBLE_DEVICEID        = 0x10;//«Device ID»	Device ID Profile v1.3 or later,«Security Manager TK Value»	Bluetooth Core Specification:
    final val EBLE_SECURITYMANAGER = 0x11;//«Security Manager Out of Band Flags»	Bluetooth Core Specification:
    final val EBLE_SLAVEINTERVALRA = 0x12;//«Slave Connection Interval Range»	Bluetooth Core Specification:
    final val EBLE_16BitSSUUID     = 0x14;//«List of 16-bit Service Solicitation UUIDs»	Bluetooth Core Specification:
    final val EBLE_128BitSSUUID    = 0x15;//«List of 128-bit Service Solicitation UUIDs»	Bluetooth Core Specification:
    final val EBLE_SERVICE_DATA_16Bit     = 0x16;//«Service Data»	Bluetooth Core Specification:​«Service Data - 16-bit UUID»	​Core Specification Supplement, Part A, section 1.11
    final val EBLE_PTADDRESS       = 0x17;//«Public Target Address»	Bluetooth Core Specification:
    final val EBLE_RTADDRESS       = 0x18;;//«Random Target Address»	Bluetooth Core Specification:
    final val EBLE_APPEARANCE      = 0x19;//«Appearance»	Bluetooth Core Specification:
    final val EBLE_DEVADDRESS      = 0x1B;//«​LE Bluetooth Device Address»	​Core Specification Supplement, Part A, section 1.16
    final val EBLE_LEROLE          = 0x1C;//«​LE Role»	​Core Specification Supplement, Part A, section 1.17
    final val EBLE_PAIRINGHASH     = 0x1D;//«​Simple Pairing Hash C-256»	​Core Specification Supplement, Part A, section 1.6
    final val EBLE_PAIRINGRAND     = 0x1E;//«​Simple Pairing Randomizer R-256»	​Core Specification Supplement, Part A, section 1.6
    final val EBLE_32BitSSUUID     = 0x1F;//​«List of 32-bit Service Solicitation UUIDs»	​Core Specification Supplement, Part A, section 1.10
    final val EBLE_SERVICE_DATA_32Bit    = 0x20;//​«Service Data - 32-bit UUID»	​Core Specification Supplement, Part A, section 1.11
    final val EBLE_SERVICE_DATA_128Bit   = 0x21;//​«Service Data - 128-bit UUID»	​Core Specification Supplement, Part A, section 1.11
    final val EBLE_SECCONCONF      = 0x22;//​«​LE Secure Connections Confirmation Value»	​Core Specification Supplement Part A, Section 1.6
    final val EBLE_SECCONRAND      = 0x23;//​​«​LE Secure Connections Random Value»	​Core Specification Supplement Part A, Section 1.6​
    final val EBLE_3DINFDATA       = 0x3D;//​​«3D Information Data»	​3D Synchronization Profile, v1.0 or later
    final val EBLE_MANUFACTURER_DATA         = 0xFF;//«Manufacturer Specific Data»	Bluetooth Core Specification:
  }


  /** Parse scan record. */
  def parseScanRecord(arr: Uint8Array): List[ScanRecordToken] = {
    val tokensAcc = List.newBuilder[ScanRecordToken]

    /** Parse full uuid from array by offset. */
    def __arrayReadUuidFull(offset: Int): ScanRecordToken.UuidToken = {
      ScanRecordToken.UuidToken(
        JsBinaryUtil.byteArrayReadUuid(arr, offset),
        UuidHelper.B16
      )
    }

    /** Parse partial UUID. */
    def __readShortenUuid(pos: Int, len: Int, helper: UuidHelper)(reader: (Uint8Array, Int) => Double): ScanRecordToken.UuidToken = {
      val part = reader(arr, pos)
      val hex = JsBinaryUtil.toHexString( part, len )
      ScanRecordToken.UuidToken(hex, helper)
    }

    val l16bit = 2
    val l32bit = l16bit * 2
    val l128bit = l32bit * l32bit

    /** Parse 2-bytes short UUID. */
    def __read16bitUuid(pos: Int): ScanRecordToken.UuidToken =
      __readShortenUuid(pos, len = l16bit, UuidHelper.B2)( JsBinaryUtil.littleEndianToUint16 )

    /** Parse 4-bytes short UUID. */
    def __read32bitUuid(pos: Int): ScanRecordToken.UuidToken =
      __readShortenUuid(pos, len = l32bit, UuidHelper.B4)( JsBinaryUtil.littleEndianToUint32 )

    /** Parse sequence of fixed-len elements. */
    def __readMany[T](pos: Int, fullDataLen: Int, oneItemLen: Int)(reader: Int => T): Iterator[T] = {
      for {
        i <- 0.to(fullDataLen, oneItemLen).iterator
      } yield {
        reader( pos + i )
      }
    }

    // Parsing on while loop over array of bytes.
    @tailrec
    def __step(pos: Int): Unit = {
      if (pos < arr.length) {
        // Starting from length byte. 0 to stop parsing.
        val tokenBytesLen = arr(pos)
        if (tokenBytesLen > 0) {
          val typePos = pos + 1
          val typeCode = arr(typePos)
          val dataPos = typePos + 1
          val dataLen = tokenBytesLen - 1

          typeCode match {

            // Short 2-byte service UUID.
            case GattTypes.EBLE_16BitUUIDInc | GattTypes.EBLE_16BitUUIDCom =>
              tokensAcc ++= __readMany(dataPos, dataLen, l16bit)(__read16bitUuid)

            // Short 4-byte service UUID.
            case GattTypes.EBLE_32BitUUIDInc | GattTypes.EBLE_32BitUUIDCom =>
              tokensAcc ++= __readMany(dataPos, dataLen, l32bit)(__read32bitUuid)

            case GattTypes.EBLE_128BitUUIDInc | GattTypes.EBLE_128BitUUIDCom =>
              tokensAcc += __arrayReadUuidFull( dataPos )

            case GattTypes.EBLE_SHORTNAME | GattTypes.EBLE_LOCALNAME =>
              val name = JsBinaryUtil.bytesUtf8ToString( new Uint8Array(arr.buffer, dataPos, dataLen) )
              tokensAcc += ScanRecordToken.LocalName(name)

            case GattTypes.EBLE_TXPOWERLEVEL =>
              val txPower = JsBinaryUtil.littleEndianToInt8( arr, dataPos )
              tokensAcc += ScanRecordToken.TxPower( txPower )

            case GattTypes.EBLE_SERVICE_DATA_16Bit =>
              val uuidTok = __read16bitUuid(dataPos)
              val data = new Uint8Array( arr.buffer, byteOffset = dataPos + l16bit, length = dataLen - l16bit)
              tokensAcc += ScanRecordToken.ServiceData(uuidTok, data)

            case GattTypes.EBLE_SERVICE_DATA_32Bit =>
              val uuidTok = __read32bitUuid(dataPos)
              val data = new Uint8Array( arr.buffer, byteOffset = dataPos + l32bit, length = dataLen - l32bit)
              tokensAcc += ScanRecordToken.ServiceData(uuidTok, data)

            case GattTypes.EBLE_SERVICE_DATA_128Bit =>
              val uuidTok = __arrayReadUuidFull( dataPos )
              val data = new Uint8Array( arr.buffer, byteOffset = dataPos + l128bit, length = dataLen - l128bit)
              tokensAcc += ScanRecordToken.ServiceData( uuidTok, data )

            case GattTypes.EBLE_MANUFACTURER_DATA =>
              val data = new Uint8Array( arr.buffer, byteOffset = dataPos, length = dataLen)
              tokensAcc += ScanRecordToken.ManufacturerData(data)

            case _ =>
              // For beacons - dont care. Just skip it.
              //println("Unknown eBLE GATT type: 0x" + JsBinaryUtil.toHexString(typeCode, 1))

          }

          // Next step, next bytes. +1 - because len byte also have its len.
          __step( pos + tokenBytesLen + 1 )
        }
      }
    }

    // Go over bytes array:
    __step(0)

    // Finally, return collected results...
    tokensAcc.result()
  }


}
