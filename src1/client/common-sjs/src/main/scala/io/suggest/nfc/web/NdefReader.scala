package io.suggest.nfc.web

import org.scalajs.dom
import org.scalajs.dom.experimental.AbortSignal

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.typedarray.DataView
import scala.scalajs.js.|

@js.native
@JSGlobal("NDEFReader")
class NdefReader() extends dom.EventTarget {

  var onreading: js.Function1[NdefReadingEvent, Unit] = js.native

  var onreadingerror: js.Function1[dom.ErrorEvent, Unit] = js.native

  def scan(options: NdefScanOptions = js.native): js.Promise[Unit] = js.native

  def write(data: String | NdefMessage,
            options: NdefWriteOptions = js.native): js.Promise[Unit] = js.native

}


trait NdefMessage extends js.Object {
  val records: js.Array[NdefRecord]
}

trait NdefScanOptions extends js.Object {
  val signal: js.UndefOr[AbortSignal] = js.undefined
}
trait NdefWriteOptions extends NdefScanOptions {
  val overwrite: js.UndefOr[Boolean] = js.undefined
}


trait NdefReadingEvent extends dom.Event {
  val message: js.Array[NdefRecord]
}


object NdefRecord {

  object RecordType {

    final def LOCAL_RECORD_PREFIX = ":"

    final def EMPTY = "empty"
    final def UNKNOWN = "unknown"
    final def TEXT = "text"
    final def URL = "url"
    final def MIME = "mime"
    final def ABSOLUTE_URL = "absolute-url"
    final def UNCHANGED = "unchanged"
    final def RESERVED = "reserved"

    object SmartPoster {

      final def SMART_POSTER = "smart-poster"

      /** Type record-type.
        * .data = encode(mime)   // Encoded MIME type of smart poster content
        */
      final def TYPE_ = LOCAL_RECORD_PREFIX + TYPE
      final def TYPE = "t"

      /** Size record-type.
        * .data = new Uint32Array([4096])  // Byte size of smart poster content
        */
      final def SIZE_ = LOCAL_RECORD_PREFIX + SIZE
      final def SIZE = "s"

      /** Action record-type.
        * .data = new Uint8Array([0])   // The action, in this case open in the browser
        */
      final def ACTION_ = LOCAL_RECORD_PREFIX + ACTION
      final def ACTION = "act"

    }

  }

}

trait NdefRecord extends js.Object {
  /** @see [[NdefRecord.RecordType]] */
  val recordType: String

  val mediaType,
      id,
      encoding,
      lang: js.UndefOr[String] = js.undefined
  val data: js.UndefOr[DataView] = js.undefined

  //TODO def toRecords?
}
