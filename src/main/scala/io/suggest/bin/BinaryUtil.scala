package io.suggest.bin

import java.nio.ByteBuffer

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 21:28
  * Description: rewrite кода evothings/util.js на чистую scala.
  *
  * @see [[https://github.com/evothings/evothings-libraries/blob/master/libs/evothings/util/util.js]]
  */
object BinaryUtil {

  /**
    * Interpret byte buffer as little endian 8 bit integer.
    * По-русски: извлечь указанный байт исходного массива байт и декодировать его как целое знаковое число.
    * {{{
    *   littleEndianToInt8(
    *     littleEndianToUint8(uint8Binary, 10)
    *   )
    * }}}
    *
    * @return Целое знаковое.
    */
  def littleEndianToInt8(x: Int): Int = {
    if ( (x & 0x80) != 0 ) x - 256 else x
  }


  /** Converts a single Base64 character to a 6-bit integer.
    *
    * @see [[https://github.com/evothings/cordova-ble/blob/master/ble.js#L450]]
    */
  def b64ToUint6(nChr: Int): Int = {
    if (nChr > 64 && nChr < 91) {
      nChr - 65
    } else if (nChr > 96 && nChr < 123) {
      nChr - 71
    } else if (nChr > 47 && nChr < 58) {
      nChr + 4
    } else if (nChr == 43) {
      62
    } else if (nChr == 47) {
      63
    } else {
      0
    }
  }


  /** Извлечь содержимое byte buffer в массив байт. */
  def byteBufToByteArray[T](bbuf: ByteBuffer): Array[Byte] = {
    val data = Array.ofDim[Byte](bbuf.remaining())
    bbuf.get(data)
    data
  }

}
