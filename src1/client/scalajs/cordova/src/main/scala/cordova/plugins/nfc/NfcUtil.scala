package cordova.plugins.nfc

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.typedarray.ArrayBuffer

@js.native
@JSGlobal("window.util")
object NfcUtil extends js.Object {

  /** @param byte <= 256 */
  def toHex(byte: Int): String = js.native
  def toPrintable(byte: Int): String = js.native
  def bytesToString(bytes: ByteArray_t): String = js.native
  def stringToBytes(str: String): ByteArray_t = js.native
  def bytesToHexString(bytes: ByteArray_t): String = js.native
  def isType(record: NdefRecord, tnf: TNF_t, `type`: NdefData_t): Boolean = js.native
  def arrayBufferToHexString(buffer: ArrayBuffer): String = js.native
  def hexStringToArrayBuffer(hexString: String): ArrayBuffer = js.native

}
