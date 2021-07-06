package io.suggest.nfc

import io.suggest.perm.IPermissionState
import japgolly.univeq.UnivEq
import org.scalajs.dom

import scala.concurrent.Future
import scala.scalajs.js.typedarray.{ArrayBufferView, DataView}

/** Interface for NFC APIs.
  * Note that implementation may be STATEFUL, if needed.
  */
trait INfcApi {

  /** Return type for implementation-side record builders for writing. */
  type WRecord_t

  def isApiAvailable(): Boolean

  def readPermissionState(): Future[IPermissionState]

  def scan(props: NfcScanProps): NfcPendingState

  def write(message: Seq[WRecord_t], options: NfcWriteOptions = NfcWriteOptions.empty): NfcPendingState

  def canMakeReadOnly: Boolean
  def makeReadOnly(): Future[Unit]

  def textRecord(text: String): WRecord_t
  def uriRecord(uri: String): WRecord_t


}


/** Interface for cross-API NDEF-record. */
trait INdefRecord {
  def tnf: Tnf
  def recordType: Option[Rtd]

  def mediaType: Option[String]
  def id: Option[String]
  def encoding: Option[String]
  def lang: Option[String]

  //def data: Option[DataView]
  def dataAsByteView(): Option[DataView]
  def dataAsString(): Option[String]
  def dataAsRecords(): Option[NdefMessage]

}


case class NdefMessage(
                        message: Seq[INdefRecord],
                      )
case class NdefScanResult(
                           message: NdefMessage,
                         )
trait INfcError {
  def message: String
  def exception: Option[Throwable]
  def domErrorEvent: Option[dom.ErrorEvent]
}
case class NfcScanProps(
                         onMessage: NdefMessage => Unit,
                         onError: Option[INfcError => Unit] = None,
                       )

/** @param result Future returns, if underlying scan() call returned.
  *               This means scanning is started or finished.
  *               Future fails, if permission denied or NFC is disabled/unavailable.
  * @param cancel Cancelling function.
  */
case class NfcPendingState(
                            result: Future[_],
                            cancel: Option[() => Unit],
                          )

object NfcWriteOptions {
  def empty = apply()
}
case class NfcWriteOptions(
                            overwrite  : Boolean = false,
                          )


sealed trait Tnf
object Tnf {
  @inline implicit def univEq: UnivEq[Tnf] = UnivEq.derive
}
object Tnfs {
  case object Empty extends Tnf
  case object WellKnown extends Tnf
  case object MimeMedia extends Tnf
  case object AbsoluteUri extends Tnf
  case class ExternalType(typeName: String) extends Tnf
  case object Unknown extends Tnf
  case object Unchanged extends Tnf
  case object Reserved extends Tnf
}


sealed trait Rtd
object Rtd {
  @inline implicit def univEq: UnivEq[Rtd] = UnivEq.derive
}
object Rtds {
  case class Local(s: String) extends Rtd
  case object Text extends Rtd
  case object Uri extends Rtd
  case object SmartPoster extends Rtd
  case object SpType extends Rtd
  case object SpSize extends Rtd
  case object SpAction extends Rtd
}
