package cordova.plugins

import scala.scalajs.js
import scala.scalajs.js.|

package object nfc {

  type TNF_t <: Int

  type ByteArray_t = js.Array[Int]
  type RTD_t = ByteArray_t

  type NdefMessage_t = js.Array[NdefRecord]

  type NdefData_t = ByteArray_t | String

  type NfcPollFlag_t = Int

}
