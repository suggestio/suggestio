package cordova.plugins.nfc

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.|

/** Scala.js API for `ndef` object.
  * The ndef object provides NDEF constants, functions for creating NdefRecords, and functions for converting data.
  *
  * @see [[https://github.com/chariotsolutions/phonegap-nfc#ndef]]
  * @see [[http://developer.android.com/reference/android/nfc/NdefRecord.html]] for documentation about constants
  */
@js.native
@JSGlobal("ndef")     // window.ndef
object Ndef extends js.Object {

  /** @see [[http://developer.android.com/reference/android/nfc/NdefRecord.html]] */
  val TNF_EMPTY,
      TNF_WELL_KNOWN,
      TNF_MIME_MEDIA,
      TNF_ABSOLUTE_URI,
      TNF_EXTERNAL_TYPE,
      TNF_UNKNOWN,
      TNF_UNCHANGED,
      TNF_RESERVED: TNF_t = js.native

  val RTD_TEXT,
      RTD_URI,
      RTD_SMART_POSTER,
      RTD_ALTERNATIVE_CARRIER,
      RTD_HANDOVER_CARRIER,
      RTD_HANDOVER_REQUEST,
      RTD_HANDOVER_SELECT: RTD_t = js.native

  def record(tnf      : TNF_t           = js.native,
             `type`   : RTD_t | String  = js.native,
             id       : NdefData_t      = js.native,
             payload  : NdefData_t      = js.native,
            ): NdefRecord = js.native

  /** Creates an NDEF record containing plain text.
    *
    * @param text String of text to encode
    * @param languageCode ISO/IANA language code. Examples: “fi”, “en-US”, “fr- CA”, “jp”. (optional)
    * @return NdefRecord instance.
    */
  def textRecord(text           : String,
                 languageCode   : String = js.native,
                 id             : NdefData_t = js.native,
                ): NdefRecord = js.native

  def uriRecord(uri: String,
                id: NdefData_t = js.native,
               ): NdefRecord = js.native

  def absoluteUriRecord(uri: String,
                        payload: NdefData_t = js.native,
                        id: NdefData_t = js.native,
                       ): NdefRecord = js.native

  def mimeMediaRecord(mimeType: String,
                      payload: NdefData_t,
                      id: NdefData_t = js.native,
                     ): NdefRecord = js.native

  def smartPoster(records: NdefMessage_t,
                  id: NdefData_t = js.native,
                 ): NdefRecord = js.native

  def emptyRecord(): NdefRecord = js.native

  def androidApplicationRecord( packageName: String ): NdefRecord = js.native

  def encodeMessage( ndefRecords: NdefMessage_t ): ByteArray_t = js.native
  def decodeMessage( ndefBytes: ByteArray_t ): NdefMessage_t = js.native

  def decodeTnf( tnfByte: TNF_t ): Tnf = js.native
  def encodeTnf( mb: Boolean, me: Boolean, cf: Boolean, sr: Boolean, il: Boolean, tnf: TNF_t ): TNF_t = js.native

  def tnfToString( tnf: TNF_t ): String = js.native

}


trait NdefRecord extends js.Object {
  val tnf: TNF_t
  val `type`: js.UndefOr[RTD_t] = js.undefined
  val id: js.UndefOr[ByteArray_t] = js.undefined
  val payload: js.UndefOr[ByteArray_t] = js.undefined
}


trait Tnf extends js.Object {
  val mb, me, cf, sr, il: Boolean
  val tnf: TNF_t
}
