package io.suggest.pick

import io.suggest.bin.BinaryUtil
import io.suggest.common.uuid.LowUuidUtil
import org.scalajs.dom.{Blob, FileReader, UIEvent}
import japgolly.univeq._

import scala.annotation.tailrec
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArray, Uint8Array}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.12.16 17:07
  * Description: JS-специфичная утиль для работы с бинарщиной.
  *
  * Код основан на evothings util.js.
  * Пришлось переписать с js на это из-за проблем со скрещиванием ProvidedJS и sbt-scalajs-bundler.
  *
  * @see [[https://github.com/evothings/evothings-libraries/blob/master/libs/evothings/util/util.js]]
  */

object JsBinaryUtil {

  type Arr_t = TypedArray[Short, _]

  /**
   * Interpret byte buffer as little endian 8 bit integer.
   * Returns converted number.
   * @param data Input buffer.
   * @param offset Start of data.
   * @return Converted number.
   */
  def littleEndianToInt8(data: Arr_t, offset: Int): Int = {
    val x = littleEndianToUint8(data, offset)
    if ((x & 0x80) > 0)
      x - 256
    else
      x
  }

  /**
    * Interpret byte buffer as unsigned little endian 8 bit integer.
    * @param data Массив [байт] исходный.
    * @param offset индекс байта.
    * @return Целое беззнаковое.
    */
  def littleEndianToUint8(data: Arr_t, offset: Int): Int = {
    data(offset)
  }

  /**
   * Interpret byte buffer as little endian 16 bit integer.
   * Returns converted number.
   * @param data Input buffer.
   * @param offset Start of data.
   * @return Converted number.
   */
  def littleEndianToInt16(data: Arr_t, offset: Int): Int = {
    (littleEndianToInt8(data, offset + 1) << 8) +
      littleEndianToUint8(data, offset)
  }


  /**
   * Interpret byte buffer as unsigned little endian 16 bit integer.
   * Returns converted number.
   * @param data Input buffer.
   * @param offset Start of data.
   * @return Converted number.
   */
  def littleEndianToUint16(data: Arr_t, offset: Int): Int = {
    (littleEndianToUint8(data, offset + 1) << 8) +
      littleEndianToUint8(data, offset)
  }


  /**
   * Interpret byte buffer as unsigned little endian 32 bit integer.
   * Returns converted number.
   * @param data Input buffer.
   * @param offset Start of data.
   * @return Converted number.
    *        Double, потому что жаба исторически не понимает uint32,
    *        а scala.js форсирует signed int поверх js number.
   */
  def littleEndianToUint32(data: Arr_t, offset: Int): Double = {
    (littleEndianToUint8(data, offset + 3) << 24) +
      (littleEndianToUint8(data, offset + 2) << 16) +
      (littleEndianToUint8(data, offset + 1) << 8) +
      littleEndianToUint8(data, offset)
  }


  /**
   * Interpret byte buffer as signed big endian 16 bit integer.
   * Returns converted number.
   * @param data Input buffer.
   * @param offset Start of data.
   * @return Converted number.
   */
  def bigEndianToInt16(data: Arr_t, offset: Int): Int = {
    (littleEndianToInt8(data, offset) << 8) +
      littleEndianToUint8(data, offset + 1)
  }


  /**
   * Interpret byte buffer as unsigned big endian 16 bit integer.
   * Returns converted number.
   * @param data Input buffer.
   * @param offset Start of data.
   * @return Converted number.
   */
  def bigEndianToUint16(data: Arr_t, offset: Int): Int = {
    (littleEndianToUint8(data, offset) << 8) +
      littleEndianToUint8(data, offset + 1)
  }

  /**
   * Interpret byte buffer as unsigned big endian 32 bit integer.
   * Returns converted number.
   * @param data Input buffer.
   * @param offset Start of data.
   * @return Converted number.
   */
  def bigEndianToUint32(data: Arr_t, offset: Int): Double = {
    (littleEndianToUint8(data, offset) << 24) +
      (littleEndianToUint8(data, offset + 1) << 16) +
      (littleEndianToUint8(data, offset + 2) << 8) +
      littleEndianToUint8(data, offset + 3)
  }


  //b64ToUint6(nChr) -- см. common BinaryUtil.


  /**
   * Decodes a Base64 string. Returns a Uint8Array.
   * nBlocksSize is optional.
   * @param sBase64 Base64 string.
   * @param nBlocksSize optional.
   * @return Uint8Array
   */
  def base64DecToArr(sBase64: String, nBlocksSize: Int = -1): Uint8Array = {
    val sB64Enc = sBase64.replace("[^A-Za-z0-9+/]", "")
    cleanBase64DecToArr(sB64Enc, nBlocksSize)
  }
  /** Конвертация чистой (без \s) base64-последовательности в Uint8Array.
    * Этим методом можно сэкономить время и ресурсы благодаря пропуску стадии очистки base64-строки с помощью регэкспа.
    */
  def cleanBase64DecToArr(cleanBase64: CharSequence, nBlocksSize: Int = -1): Uint8Array = {
    val nInLen = cleanBase64.length
    val nOutLen = if (nBlocksSize > 0) {
      Math.ceil((nInLen * 3 + 1 >> 2) / nBlocksSize).toInt * nBlocksSize
    } else {
      nInLen * 3 + 1 >> 2
    }
    val taBytes = new Uint8Array(nOutLen)

    var nMod4, nUint24, nOutIdx = 0

    // Криво имитируем for-loop...
    @tailrec
    def _loop0(nInIdx: Int = 0): Unit = {
      if (nInIdx < nInLen) {
        nMod4 = nInIdx & 3
        nUint24 |= BinaryUtil.b64ToUint6(cleanBase64.charAt(nInIdx).toInt) << 18 - 6 * nMod4
        if ((nMod4 ==* 3) || (nInLen - nInIdx ==* 1)) {
          @tailrec
          def _loop1(nMode3: Int = 0): Unit = {
            if (nMode3 < 3 && nOutIdx < nOutLen) {
              taBytes(nOutIdx) = (nUint24 >>> (16 >>> nMode3 & 24) & 255).asInstanceOf[Short]
              // re-call loop1...
              nOutIdx += 1
              _loop1(nMode3 + 1)
            }
          }
          _loop1()
          nUint24 = 0
        }
        _loop0(nInIdx + 1)
      }
    }

    // run loop
    _loop0()

    // return taBytes.
    taBytes
  }

  /**
    * Returns the integer i in hexadecimal string form,
    * with leading zeroes, such that
    * the resulting string is at least byteCount*2 characters long.
    * @param i Integer number.
    * @param byteCount Bytes count.
    * @return hex string.
    */
  def toHexString(i: Double, byteCount: Int): String = {
    import scalajs.js.JSNumberOps._

    var string = i.toString(16)
    val finalLen = byteCount * 2
    // Добить нулями слева, когда это требуется
    val leadingZeroesCount = finalLen - string.length
    if (leadingZeroesCount > 0)
      string = ("0" * leadingZeroesCount) + string
    string
  }


  /**
    * Takes a ArrayBuffer or TypedArray and returns its hexadecimal representation.
    * No spaces or linebreaks.
    * @param data byte array.
    * @return hex string.
    */
  def typedArrayToHexString(data: Arr_t): String = {
    // view data as a Uint8Array, unless it already is one.
    var str = ""
    var i = 0
    while (i < data.length) {
      str += toHexString(data(i), 1)
      i += 1
    }
    str
  }
  def typedArrayToHexString(buf: ArrayBuffer): String = {
    typedArrayToHexString( new Uint8Array(buf) )
  }


  /** Прочитать указанный Blob в новый ArrayBuffer.
    *
    * Следует аккуратнее дёргать этот метод на реальных задачах:
    * метод приводит к загрузке всего бинаря целиком в память JS VM, а бинарь может быть очень большим...
    *
    * @param blob Исходный блоб, подлежащий чтению.
    * @see [[https://stackoverflow.com/a/15981017]]
    * @return Инстанс ArrayBuffer.
    */
  def blob2arrBuf(blob: Blob): Future[ArrayBuffer] = {
    val fr = new FileReader()
    val p = Promise[ArrayBuffer]()
    fr.onload = { _: UIEvent =>
      p.success( fr.result.asInstanceOf[ArrayBuffer] )
    }
    fr.readAsArrayBuffer(blob)
    p.future
  }


  /** Десериализация 16 байтов uuid из js-массива байт.
    *
    * @param array Массив байт.
    * @param offset Начальный сдвиг в массиве байт.
    * @return Строка UUID стандартного вида "хххххххх-хххх-хххх-хххх-хххххххххххх".
    */
  def byteArrayReadUuid(array: Uint8Array, offset: Int): String = {
    val uuidFormat = LowUuidUtil.UUID_FORMAT
    val sb = new StringBuilder( uuidFormat.sum + uuidFormat.length - 1 )
    val delim = LowUuidUtil.UID_PARTS_DELIM.head
    var k = offset
    for (uuidPartLen <- uuidFormat) {
      for (_ <- 0 until uuidPartLen) {
        val chStr = JsBinaryUtil.toHexString( array(k), 1 )
        sb.append( chStr )
        k += 1
      }
      // Для всех частей кроме последней надо добавлять разделитель "-".
      if (uuidPartLen < 6)
        sb.append( delim )
    }
    sb.toString()
  }


  /** Наиболее быстрый метод десериализации - застаить браузер декодировать массив.
    * Как это просто и легко сделать на scala.js - загадка.
    * Поэтому, тут просто костыль, вызывающий String.fromCharCode(ch*).
    */
  def bytesUtf8ToString(a: Uint8Array): String = {
    // TODO В оригинале было decodeURIComponent(escape(...)). Т.е. escape+unescape. Зачем-то это надо было. Не ясно, зачем именно.
    js.Dynamic.global
      .String.fromCharCode
      .apply(null, a)
      .asInstanceOf[String]
  }

}

