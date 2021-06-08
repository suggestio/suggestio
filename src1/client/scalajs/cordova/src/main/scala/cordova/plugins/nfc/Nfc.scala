package cordova.plugins.nfc

import io.suggest.sjs.JsApiUtil
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import io.suggest.err.ToThrowableJs._

import scala.concurrent.Future
import scala.scalajs.js.{JavaScriptException, |}
import scala.util.{Failure, Success}

/** API for NFC global object, provided by phonegap-nfc plugin.
  * The `nfc` object provides access to the device's NFC sensor.
  *
  * @see [[https://github.com/chariotsolutions/phonegap-nfc#nfc]]
  */
@js.native
trait INfc extends js.Object {

  def addNdefListener( callback: js.Function1[NfcEvent, Unit],
                       success: js.Function0[Unit] = js.native,
                       error: js.Function1[js.Any, Unit] = js.native,
                     ): Unit = js.native
  def removeNdefListener( callback: js.Function1[NfcEvent, Unit],
                          success: js.Function0[Unit] = js.native,
                          error: js.Function1[js.Any, Unit] = js.native,
                        ): Unit = js.native

  def addTagDiscoveredListener( callback: js.Function1[NfcEvent, Unit],
                                success: js.Function0[Unit] = js.native,
                                error: js.Function1[js.Any, Unit] = js.native,
                              ): Unit = js.native
  def removeTagDiscoveredListener( callback: js.Function1[NfcEvent, Unit],
                                   success: js.Function0[Unit] = js.native,
                                   error: js.Function1[js.Any, Unit] = js.native,
                                 ): Unit = js.native

  def addMimeTypeListener( mimeType: String,
                           callback: js.Function1[NfcEvent, Unit],
                           success: js.Function0[Unit] = js.native,
                           error: js.Function1[js.Any, Unit] = js.native,
                         ): Unit = js.native
  def removeMimeTypeListener( mimeType: String,
                              callback: js.Function1[NfcEvent, Unit],
                              success: js.Function0[Unit] = js.native,
                              error: js.Function1[js.Any, Unit] = js.native,
                            ): Unit = js.native

  def addNdefFormatableListener( callback: js.Function1[NfcEvent, Unit],
                                 success: js.Function0[Unit] = js.native,
                                 error: js.Function1[js.Any, Unit] = js.native,
                               ): Unit = js.native

  def write( message: NdefMessage_t,
             success: js.Function0[Unit] = js.native,
             error: js.Function1[js.Any, Unit] = js.native,
           ): Unit = js.native

  def makeReadOnly(success: js.Function0[Unit] = js.native,
                   error: js.Function1[js.Any, Unit] = js.native,
                  ): Unit = js.native

  def share( message: NdefMessage_t,
             success: js.Function0[Unit] = js.native,
             error: js.Function1[js.Any, Unit] = js.native,
           ): Unit = js.native
  def unshare(success: js.Function0[Unit] = js.native,
              error: js.Function1[js.Any, Unit] = js.native,
             ): Unit = js.native

  def erase(success: js.Function0[Unit] = js.native,
            error: js.Function1[js.Any, Unit] = js.native,
           ): Unit = js.native

  def handover( contentUri: String | js.Array[String],
                success: js.Function0[Unit] = js.native,
                error: js.Function1[js.Any, Unit] = js.native,
              ): Unit = js.native
  def stopHandover( success: js.Function0[Unit] = js.native,
                    error: js.Function1[js.Any, Unit] = js.native,
                  ): Unit = js.native

  def showSettings( success: js.Function0[Unit] = js.native,
                    error: js.Function1[js.Any, Unit] = js.native,
                  ): Unit = js.native

  /** Check if NFC is available and enabled on this device.
    * @param success The callback that is called when NFC is enabled.
    * @param error The callback that is called when NFC is disabled or missing.
    */
  def enabled( success: js.Function0[Unit] = js.native,
               error: js.Function1[js.Any, Unit] = js.native,
             ): Unit = js.native

  def beginSession( success: js.Function0[Unit] = js.native,
                    error: js.Function1[js.Any, Unit] = js.native,
                  ): Unit = js.native
  def invalidateSession( success: js.Function0[Unit] = js.native,
                         error: js.Function1[js.Any, Unit] = js.native,
                       ): Unit = js.native

  def scanNdef(options: ScanNdefOpts = js.native): js.Promise[NfcEventTag] = js.native
  def scanTag(): js.Promise[NfcEventTag] = js.native
  def cancelScan(): js.Promise[_] = js.native

  val FLAG_READER_NFC_A,
      FLAG_READER_NFC_B,
      FLAG_READER_NFC_F,
      FLAG_READER_NFC_V,
      FLAG_READER_NFC_BARCODE,
      FLAG_READER_SKIP_NDEF_CHECK,
      FLAG_READER_NO_PLATFORM_SOUNDS: NfcPollFlag_t = js.native

  def readerMode(flags: NfcPollFlag_t,
                 success: js.Function1[NfcEventTag, Unit] = js.native,
                 error: js.Function1[js.Any, Unit] = js.native,
                ): Unit = js.native

  def disableReaderMode( success: js.Function0[Unit] = js.native,
                         error: js.Function1[js.Any, Unit] = js.native,
                       ): Unit = js.native

}

object INfc {
  implicit final class NfcScalaExt( private val nfc: INfc ) extends AnyVal {

    def addNdefListenerF(callback: js.Function1[NfcEvent, Unit] ): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.addNdefListener( callback, _, _ ) )
    def removeNdefListenerF(callback: js.Function1[NfcEvent, Unit] ): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.removeNdefListener( callback, _, _ ) )

    def addTagDiscoveredListenerF(callback: js.Function1[NfcEvent, Unit] ): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.addTagDiscoveredListener( callback, _, _ ) )
    def removeTagDiscoveredListenerF(callback: js.Function1[NfcEvent, Unit] ): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.removeTagDiscoveredListener( callback, _, _ ) )

    def addMimeTypeListenerF( mimeType: String )(callback: js.Function1[NfcEvent, Unit]): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.addMimeTypeListener(mimeType, callback, _, _) )
    def removeMimeTypeListenerF( mimeType: String)(callback: js.Function1[NfcEvent, Unit]): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.removeMimeTypeListener(mimeType, callback, _, _) )

    def addNdefFormatableListenerF( callback: js.Function1[NfcEvent, Unit] ): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.addNdefFormatableListener( callback, _, _ ) )

    def writeF( message: NdefMessage_t ): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.write(message, _, _) )

    def makeReadOnlyF(): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.makeReadOnly )

    def shareF( message: NdefMessage_t ): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.share(message, _, _) )
    def unshareF(): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.unshare )

    def eraseF(): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.erase )

    def handoverF( contentUri: String | js.Array[String] ): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.handover(contentUri, _, _) )
    def stopHandoverF(): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.stopHandover )

    def showSettingsF(): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.showSettings )

    def enabledF(): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.enabled )

    def isEnabledAvailableF(): Future[Boolean] = {
      enabledF().transform {
        case Success(_) => Success(true)
        case Failure(_: JavaScriptException) => Success(false)
        case Failure(ex) => Failure(ex)
      }
    }

    def beginSessionF(): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.beginSession )
    def invalidateSessionF(): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.invalidateSession )

    def readerModeF( flags: NfcPollFlag_t ): Future[NfcEventTag] =
      JsApiUtil.call1ErrFut( nfc.readerMode(flags, _, _) )
    def disableReaderModeF(): Future[Unit] =
      JsApiUtil.call0ErrFut( nfc.disableReaderMode )

  }
}


@js.native
@JSGlobal("nfc")    // window.nfc
object Nfc extends INfc

trait ScanNdefOpts extends js.Object {
  val keepSessionOpen: js.UndefOr[Boolean] = js.undefined
}