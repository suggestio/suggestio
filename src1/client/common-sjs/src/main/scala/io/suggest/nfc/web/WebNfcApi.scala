package io.suggest.nfc.web

import io.suggest.msg.ErrorMsgs
import io.suggest.nfc
import io.suggest.perm.{Html5PermissionApi, IPermissionState}
import io.suggest.sjs.JsApiUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.dom2.TextDecoder
import japgolly.univeq._
import org.scalajs.dom
import org.scalajs.dom.ErrorEvent
import org.scalajs.dom.experimental.AbortController

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.typedarray.DataView


/** INfcApi implementation over WebNFC W3C standard.
  * WebNFC support already present in Chrome 89 for Android.
  */
final class WebNfcApi extends nfc.INfcApi {

  import NdefRecord.RecordType.LOCAL_RECORD_PREFIX

  override def isApiAvailable(): Boolean = {
    // Feature detection: if ('NDEFReader' in window)
    JsApiUtil.isDefinedSafe( js.constructorOf[NdefReader] )
  }

  override def readPermissionState(): Future[IPermissionState] =
    Html5PermissionApi.getPermissionState( Html5PermissionApi.PermissionName.NFC )


  def recordType2TnfRtd(wnRecordType: String): (nfc.Tnf, Option[nfc.Rtd]) = {
    val W = NdefRecord.RecordType
    val T = nfc.Tnfs
    val R = nfc.Rtds

    if (wnRecordType startsWith LOCAL_RECORD_PREFIX)          T.WellKnown -> Some {
      val recordTypeRoot = wnRecordType.tail
      if (recordTypeRoot ==* W.SmartPoster.TYPE)              R.SpType
      else if (recordTypeRoot ==* W.SmartPoster.SIZE)         R.SpSize
      else if (recordTypeRoot ==* W.SmartPoster.ACTION)       R.SpAction
      else                                                    R.Local( recordTypeRoot )
    } else if (wnRecordType ==* W.URL)                        T.WellKnown -> Some( R.Uri )
    else if (wnRecordType ==* W.SmartPoster.SMART_POSTER)     T.WellKnown -> Some( R.SmartPoster )
    else {
      val tnf = if (wnRecordType ==* W.MIME)                  T.MimeMedia
      else if (wnRecordType ==* W.ABSOLUTE_URL)               T.AbsoluteUri
      else if (wnRecordType ==* W.EMPTY)                      T.Empty
      else if (wnRecordType ==* W.UNCHANGED)                  T.Unchanged
      else if (wnRecordType ==* W.UNKNOWN)                    T.Unknown
      else                                                    T.ExternalType( wnRecordType )
      tnf -> None
    }
  }


  def tnfRtd2RecordType(tnf: nfc.Tnf, rtdOpt: Option[nfc.Rtd]): String = {
    val W = NdefRecord.RecordType
    tnf match {
      case nfc.Tnfs.WellKnown =>
        val rtd = rtdOpt getOrElse {
          throw new IllegalArgumentException( (tnf, rtdOpt).toString() )
        }
        rtd match {
          case nfc.Rtds.Uri                   => W.URL
          case nfc.Rtds.Text                  => W.TEXT
          case nfc.Rtds.SmartPoster           => W.SmartPoster.SMART_POSTER
          case other =>
            LOCAL_RECORD_PREFIX + (other match {
              case nfc.Rtds.SpType                => W.SmartPoster.TYPE
              case nfc.Rtds.SpSize                => W.SmartPoster.SIZE
              case nfc.Rtds.SpAction              => W.SmartPoster.ACTION
              case nfc.Rtds.Local( recType )      => recType
              case _                              => throw new IllegalStateException( (ErrorMsgs.SHOULD_NEVER_HAPPEN, other).toString() )
            })
        }
      case nfc.Tnfs.MimeMedia                 => W.MIME
      case nfc.Tnfs.AbsoluteUri               => W.ABSOLUTE_URL
      case nfc.Tnfs.Empty                     => W.EMPTY
      case nfc.Tnfs.ExternalType( typeName )  => typeName
      case nfc.Tnfs.Unchanged                 => W.UNCHANGED
      case nfc.Tnfs.Unknown                   => W.UNKNOWN
      case nfc.Tnfs.Reserved                  => W.RESERVED
    }
  }


  override def scan(props: nfc.NfcScanProps): nfc.NfcPendingState = {
    val abrtCtl = new AbortController

    val scanFut = Future {
      // Start scanning.
      val ndefReader = new NdefReader()
      for {
        // Ensure, we have scanning persmissions, NFC powered on and in SENCE:
        _ <- ndefReader
          .scan(
            options = new NdefScanOptions {
              override val signal = abrtCtl.signal
            }
          )
          .toFuture
      } yield {
        // Subscribe to ndef reader events:
        ndefReader.onreading = props.onMessage.compose[NdefReadingEvent] { ndefReadEvent =>
          nfc.NdefMessage(
            message = ndefReadEvent
              .message
              .iterator
              .map { ndefRecord =>
                val (_tnf, _rtdOpt) = recordType2TnfRtd( ndefRecord.recordType )

                new nfc.INdefRecord {
                  override def tnf = _tnf
                  override def recordType = _rtdOpt
                  override def mediaType = ndefRecord.mediaType.toOption
                  override def id = ndefRecord.id.toOption
                  override def encoding = ndefRecord.encoding.toOption
                  override def lang = ndefRecord.lang.toOption

                  override def data: Option[DataView] = ndefRecord.data.toOption
                  override def dataAsString = {
                    for (dataView <- data) yield {
                      new TextDecoder(
                        encoding = ndefRecord.encoding getOrElse TextDecoder.Encoding.UTF8,
                      )
                        .decode( dataView )
                    }
                  }
                }
              }
              .to( LazyList )
          )
        }

        for (onError <- props.onError) {
          ndefReader.onreadingerror = onError.compose[dom.ErrorEvent] { errorEvent =>
            new nfc.INfcError {
              override def message = errorEvent.message
              override def exception = None
              override def domErrorEvent: Option[ErrorEvent] = Some( errorEvent )
            }
          }
        }
      }
    }
      .flatten

    nfc.NfcPendingState(
      result = scanFut,
      cancel = Some( abrtCtl.abort )
    )
  }


  override def write(props: nfc.NfcWriteProps): nfc.NfcPendingState = {
    val abrtCtl = new AbortController

    val writeFut = Future {
      val ndefReader = new NdefReader()
        ndefReader
          .write(
            data = new NdefMessage {
              override val records: js.Array[NdefRecord] = {
                props
                  .message
                  .iterator
                  .map { iNdefRecord =>
                    new NdefRecord {
                      override val recordType = tnfRtd2RecordType( iNdefRecord.tnf, iNdefRecord.recordType )
                      override val encoding = iNdefRecord.encoding.orUndefined
                      override val data = iNdefRecord.data.orUndefined
                      override val mediaType = iNdefRecord.mediaType.orUndefined
                      override val id = iNdefRecord.id.orUndefined
                      override val lang = iNdefRecord.lang.orUndefined
                    }
                  }
                  .toJSArray
              }
            },
            options = new NdefWriteOptions {
              override val overwrite = props.overwrite
              override val signal = abrtCtl.signal
            }
          )
          .toFuture
    }
      .flatten

    nfc.NfcPendingState(
      result = writeFut,
      cancel = Some( abrtCtl.abort )
    )
  }

  override def canMakeReadOnly = false
  override def makeReadOnly(): Future[Unit] =
    Future.failed( new UnsupportedOperationException )

}
