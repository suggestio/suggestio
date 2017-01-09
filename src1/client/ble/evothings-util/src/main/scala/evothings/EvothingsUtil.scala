package evothings

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.typedarray.{ArrayBuffer, Uint8Array}
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 22:17
  * Description: Scala.js APIs for evothings/util.js.
  */
@JSName("evothings.util")
@js.native
object EvothingsUtil extends js.Object {

  def littleEndianToInt8(data: Uint8Array, offset: Int): Int = js.native

  def littleEndianToUint8(data: Uint8Array, offset: Int): Int = js.native

  def littleEndianToInt16(data: Uint8Array, offset: Int): Int = js.native

  def littleEndianToUint16(data: Uint8Array, offset: Int): Int = js.native

  // TODO s/Double/Long/
  def littleEndianToUint32(data: Uint8Array, offset: Int): Double = js.native

  def bigEndianToInt16(data: Uint8Array, offset: Int): Int = js.native
  def bigEndianToUint16(data: Uint8Array, offset: Int): Int = js.native

  def base64DecToArr(sBase64: String, nBlocksSize: Int = js.native): Uint8Array = js.native

  def toHexString(i: Int, byteCount: Int): String = js.native

  def typedArrayToHexString(data: Uint8Array | ArrayBuffer): String = js.native

}
