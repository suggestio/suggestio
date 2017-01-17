package io.suggest.sjs.common.bin

import io.suggest.bin.BinaryUtil

import scala.annotation.tailrec
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
	 * @param sBase64
	 * @param nBlocksSize
	 * @return Uint8Array
	 */
	def base64DecToArr(sBase64: String, nBlocksSize: Int = -1): Uint8Array = {
		val sB64Enc = sBase64.replace("[^A-Za-z0-9+/]", "")
		val nInLen = sB64Enc.length
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
				nUint24 |= BinaryUtil.b64ToUint6(sB64Enc.charAt(nInIdx).toInt) << 18 - 6 * nMod4
				if (nMod4 == 3 || nInLen - nInIdx == 1) {
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
	def toHexString(i: Int, byteCount: Int): String = {
		import scalajs.js.JSNumberOps._

		var string = i.toString(16)
		while (string.length < byteCount*2) {
			string = "0" + string
		}
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

}
