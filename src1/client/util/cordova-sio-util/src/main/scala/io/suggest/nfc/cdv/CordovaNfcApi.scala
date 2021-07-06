package io.suggest.nfc.cdv

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.nfc
import io.suggest.perm.{BoolOptPermissionState, IPermissionState}
import io.suggest.sjs.JsApiUtil
import cordova.plugins.{nfc => cdv}
import diode.data.Pot
import io.suggest.common.empty.OptionUtil
import io.suggest.dev.{MOsFamilies, MOsFamily}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.nfc.{INfcError, NdefMessage, NfcPendingState, NfcScanProps, NfcWriteOptions}
import japgolly.univeq._

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.typedarray.{DataView, Uint8Array}


object CordovaNfcApi extends Log {

  /** Make API instance for current OS platform. */
  def apply(osFamily: MOsFamily): CordovaNfcApi = {
    osFamily match {
      case MOsFamilies.Android        => new AndroidNfcApi
      case MOsFamilies.Apple_iOS      => new IosNfcApi
    }
  }

  /** Convert TNF from [common] nfc format to phonegap-nfc format. */
  def nfc2cdvTnf(nfcTnf: nfc.Tnf): cdv.TNF_t = {
    nfcTnf match {
      case nfc.Tnfs.WellKnown         => cdv.Ndef.TNF_WELL_KNOWN
      case nfc.Tnfs.MimeMedia         => cdv.Ndef.TNF_MIME_MEDIA
      case nfc.Tnfs.AbsoluteUri       => cdv.Ndef.TNF_ABSOLUTE_URI
      case nfc.Tnfs.ExternalType(_)   => cdv.Ndef.TNF_EXTERNAL_TYPE
      case nfc.Tnfs.Unchanged         => cdv.Ndef.TNF_UNCHANGED
      case nfc.Tnfs.Empty             => cdv.Ndef.TNF_EMPTY
      case nfc.Tnfs.Unknown           => cdv.Ndef.TNF_UNKNOWN
      case nfc.Tnfs.Reserved          => cdv.Ndef.TNF_RESERVED
    }
  }

  /** Convert TNF from phonegap-nfc format to [common] nfc format. */
  def cdv2nfcTnf(tnf: cdv.TNF_t, recordType: js.UndefOr[cdv.RTD_t]): nfc.Tnf = {
    tnf match {
      case cdv.Ndef.TNF_WELL_KNOWN    => nfc.Tnfs.WellKnown
      case cdv.Ndef.TNF_MIME_MEDIA    => nfc.Tnfs.MimeMedia
      case cdv.Ndef.TNF_ABSOLUTE_URI  => nfc.Tnfs.AbsoluteUri
      case cdv.Ndef.TNF_EXTERNAL_TYPE => nfc.Tnfs.ExternalType( cdv.NfcUtil.bytesToString( recordType.get ) )
      case cdv.Ndef.TNF_UNCHANGED     => nfc.Tnfs.Unchanged
      case cdv.Ndef.TNF_EMPTY         => nfc.Tnfs.Empty
      case cdv.Ndef.TNF_UNKNOWN       => nfc.Tnfs.Unknown
      case cdv.Ndef.TNF_RESERVED      => nfc.Tnfs.Reserved
    }
  }

  /** Convert RTD from phonegap-nfc to common format. */
  def cdv2nfcRtd(rtd: cdv.RTD_t): nfc.Rtd = {
    rtd match {
      case cdv.Ndef.RTD_URI                                 => nfc.Rtds.Uri
      case cdv.Ndef.RTD_TEXT                                => nfc.Rtds.Text
      case cdv.Ndef.RTD_SMART_POSTER                        => nfc.Rtds.SmartPoster
      // TODO case Handover, etc...
      case other =>
        val recordTypeStr = cdv.NfcUtil.bytesToString( other )
        val R = nfc.web.NdefRecord.RecordType
        if (recordTypeStr startsWith R.LOCAL_RECORD_PREFIX) {
          val localRecordType = recordTypeStr.tail
          if (localRecordType ==* R.SmartPoster.TYPE)          nfc.Rtds.SpType
          else if (localRecordType ==* R.SmartPoster.SIZE)     nfc.Rtds.SpSize
          else if (localRecordType ==* R.SmartPoster.ACTION)   nfc.Rtds.SpAction
          else                                                 nfc.Rtds.Local( localRecordType )
        } else {
          throw new UnsupportedOperationException( (nfc.Rtds, rtd).toString() )
        }
    }
  }


  /** Convert phonegap-nfc NDEF record into common i.s.nfc format. */
  def cdv2NfcNdefRecord(cdvNdefRec: cdv.NdefRecord): nfc.INdefRecord = {
    val _tnf = cdv2nfcTnf( cdvNdefRec.tnf, cdvNdefRec.`type` )

    val _recordTypeOpt = cdvNdefRec.`type`
      .toOption
      .filter { _ =>
        _tnf ==* nfc.Tnfs.WellKnown
      }
      .map( cdv2nfcRtd )

    new nfc.INdefRecord {
      override def tnf = _tnf
      override def recordType = _recordTypeOpt

      override lazy val id: Option[String] = {
        cdvNdefRec.id
          .toOption
          .map( cdv.NfcUtil.bytesToString )
      }

      override def dataAsByteView(): Option[DataView] = {
        cdvNdefRec
          .payload
          .toOption
          .map { m =>
            val uint8Arr = Uint8Array.from( m.asInstanceOf[js.Array[Short]] )
            new DataView( uint8Arr.buffer )
          }
      }

      override def dataAsString(): Option[String] = {
        cdvNdefRec.payload
          .toOption
          .map(cdv.NfcUtil.bytesToString)
      }

      override def dataAsRecords(): Option[NdefMessage] = ???

      override def mediaType: Option[String] = ???
      override def encoding: Option[String] = ???
      override def lang: Option[String] = ???
    }
  }


  /** Convert phonegap-nfc NDEF message info common i.s.nfc format. */
  def cdv2NfcNdefMessage(cdvNdefMessage: cdv.NdefMessage_t): nfc.NdefMessage = {
    nfc.NdefMessage(
      message = cdvNdefMessage
        .iterator
        .map( CordovaNfcApi.cdv2NfcNdefRecord )
        .to( LazyList )
    )
  }
  def cdv2NfcNdefMessage(cdvNdefMessageU: js.UndefOr[cdv.NdefMessage_t]): nfc.NdefMessage = {
    cdvNdefMessageU.fold {
      logger.warn( ErrorMsgs.UNEXPECTED_EMPTY_DOCUMENT, msg = cdvNdefMessageU )
      NdefMessage( Nil )
    } { cdv2NfcNdefMessage }
  }

}


/** Android and iOS implementations differs. */
trait CordovaNfcApi extends nfc.INfcApi {

  override def isApiAvailable(): Boolean =
    JsApiUtil.isDefinedSafe( cdv.Nfc )

  override def readPermissionState(): Future[IPermissionState] =
    Future successful BoolOptPermissionState( OptionUtil.SomeBool.someTrue )

  override def canMakeReadOnly: Boolean = true
  override def makeReadOnly(): Future[Unit] =
    cdv.Nfc.makeReadOnlyF()

  override type WRecord_t = cdv.NdefRecord

  override def textRecord(text: String): WRecord_t =
    cdv.Ndef.textRecord( text )

  override def uriRecord(uri: String): WRecord_t =
    cdv.Ndef.uriRecord( uri )

}


/** phonegap-nfc implementation of NFC-API for iOS platform. */
class IosNfcApi extends CordovaNfcApi {

  override def scan(props: NfcScanProps): NfcPendingState = {
    NfcPendingState(
      result = Future {
        for {
          // Open NFC session for scanning:
          nfcTag <- cdv.Nfc.scanNdef().toFuture
        } yield {
          nfcTag.ndefMessage
        }
      }
        .flatten
        .andThen { case tryRes =>
          try {
            tryRes.fold(
              {ex =>
                for (onError <- props.onError) {
                  onError( new INfcError {
                    override def message = ex.getMessage
                    override def exception = Some(ex)
                    override def domErrorEvent = None
                  })
                }
              },
              CordovaNfcApi.cdv2NfcNdefMessage
            )

          } finally {
            // Invalidate the NFC session started by scanNdef or scanTag.
            cdv.Nfc.cancelScan()
          }
        },
      cancel = Some( cdv.Nfc.cancelScan ),
    )
  }


  override def write(message: Seq[WRecord_t], options: NfcWriteOptions): NfcPendingState = ???

}


/** Android implementation for NFC API. */
class AndroidNfcApi extends CordovaNfcApi {

  /** Currently installed NDEF event listener, if any. */
  private var _ndefListener = Pot.empty[js.Function1[cdv.NfcEvent, Unit]]


  /** Remove scanning NDEF-events listener, if any. */
  def unScan(): Option[Future[_]] = {
    for {
      f <- _ndefListener.toOption
      if !_ndefListener.isPending
    } yield {
      val unListenFut = cdv.Nfc
        .removeNdefListenerF( f )
        .andThen { case tryRes =>
          // Update current NDEF-listener state:
          _ndefListener = tryRes.fold(
            _ndefListener.fail,
            _ => Pot.empty
          )
        }

      _ndefListener = _ndefListener.pending()

      unListenFut
    }
  }


  override def scan(props: nfc.NfcScanProps): nfc.NfcPendingState = {
    val fut: Future[Unit] = {
      if (
        !_ndefListener.isPending &&
        _ndefListener.isEmpty
      ) {
        val onMessageF = props.onMessage
        val listenerF = { nfcEvent: cdv.NfcEvent =>
          val nfcMsg = CordovaNfcApi.cdv2NfcNdefMessage( nfcEvent.tag.ndefMessage )
          onMessageF( nfcMsg )
        }

        // Subscribe for NDEF events:
        val listenFut = cdv.Nfc
          .addNdefListenerF( listenerF )
          .andThen { case tryRes =>
            // Save installed NDEF-listener instance.
            _ndefListener = tryRes.fold(
              _ndefListener.fail,
              _ => _ndefListener.ready( listenerF )
            )
          }

        _ndefListener = _ndefListener.pending()

        listenFut
      } else {
        Future.successful( () )
      }
    }

    nfc.NfcPendingState( fut, Some(unScan) )
  }


  override def write(message: Seq[WRecord_t], options: NfcWriteOptions): NfcPendingState = {
    ???
  }

}
