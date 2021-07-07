package io.suggest.nfc

import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.perm.{BoolOptPermissionState, IPermissionState}
import io.suggest.sjs.dom2.DomQuick

import scala.concurrent.{CancellationException, Future}
import scala.util.Random


/** Fake NFC API implementation for testing purposes. */
class FakeNfcApiImpl extends INfcApi {

  private def _emulateOperation(timeoutMs: Int = 1500): NfcPendingState = {
    val p = DomQuick.timeoutPromise( timeoutMs )
    NfcPendingState(
      result = p.fut,
      cancel = Some { () =>
        DomQuick.clearTimeout( p.timerId )
        p.promise.tryFailure( new CancellationException )
      },
    )
  }

  override type WRecord_t = None.type

  override def isApiAvailable() = true

  override def readPermissionState(): Future[IPermissionState] =
    Future successful BoolOptPermissionState( Some(true) )

  override def scan(props: NfcScanProps): NfcPendingState = {
    val r = _emulateOperation()

    for (_ <- r.result)
      props.onMessage( NdefMessage(Nil) )

    for (ex <- r.result.failed; onError <- props.onError)
      onError( new INfcError {
        override def message = ex.getMessage
        override def exception = Some(ex)
        override def domErrorEvent = None
      })

    r
  }


  override def write(message: Seq[WRecord_t], options: NfcWriteOptions): NfcPendingState =
    _emulateOperation()

  override def canMakeReadOnly: Boolean =
    new Random( System.currentTimeMillis() ).nextBoolean()

  override def makeReadOnly(): NfcPendingState =
    _emulateOperation()

  override def textRecord(text: String): WRecord_t = None
  override def uriRecord(uri: String): WRecord_t = None
  override def androidApplicationRecord(packageName: String): WRecord_t = None

}
