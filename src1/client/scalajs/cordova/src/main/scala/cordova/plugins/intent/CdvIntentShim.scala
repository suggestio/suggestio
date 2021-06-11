package cordova.plugins.intent

import io.suggest.sjs.JsApiUtil

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import io.suggest.err.ToThrowableJs._

import scala.scalajs.js.|

@js.native
@JSGlobal("window.plugins.intentShim")
object CdvIntentShim extends ICdvIntentShim


@js.native
sealed trait ICdvIntentShim extends js.Object {

  val ACTION_SEND,
      ACTION_VIEW,
      ACTION_INSTALL_PACKAGE,
      ACTION_UNINSTALL_PACKAGE,
      EXTRA_TEXT,
      EXTRA_SUBJECT,
      EXTRA_STREAM,
      EXTRA_EMAIL,
      ACTION_CALL,
      ACTION_SENDTO,
      ACTION_GET_CONTENT,
      ACTION_PICK: String = js.native

  val RESULT_CANCELED,
      RESULT_OK: Int = js.native


  def registerBroadcastReceiver(filters: Filters, callback: js.Function0[_]): Unit = js.native

  def unregisterBroadcastReceiver(): Unit = js.native

  def sendBroadcast(action: String,
                    extras: js.Dictionary[js.Any],
                    success: js.Function0[_],
                    error: js.Function1[js.Any, _]): Unit = js.native

  def onIntent(callback: js.Function1[Intent, _]): Unit = js.native
  def startActivity(intent: Intent, success: js.Function0[_], error: js.Function1[js.Any, _]): Unit = js.native

  def getIntent(callback: js.Function1[Intent, _], error: js.Function1[js.Any, _]): Unit = js.native

  def startActivityForResult(intent: Intent, callback: js.Function1[Intent, _], error: js.Function1[js.Any, _]): Unit = js.native

  def sendResult(args: Intent, callback: js.Function0[_]): Unit = js.native

}
object ICdvIntentShim {

  def isApiAvailable(): Boolean =
    JsApiUtil.isDefinedSafe( CdvIntentShim )

  implicit final class CdvIntentShimExt( private val shim: ICdvIntentShim ) extends AnyVal {

    def registerBroadcastReceiverF(filters: Filters): Future[Unit] =
      JsApiUtil.call0Fut( shim.registerBroadcastReceiver(filters, _) )

    def sendBroadcastF(action: String, extras: js.Dictionary[js.Any]): Future[Unit] =
      JsApiUtil.call0ErrFut( shim.sendBroadcast(action, extras, _, _) )

    def startActivityF(intent: Intent): Future[Unit] =
      JsApiUtil.call0ErrFut( shim.startActivity(intent, _, _) )

    def getIntentF(): Future[Intent] =
      JsApiUtil.call1ErrFut( shim.getIntent )

    def startActivityForResultF(intent: Intent): Future[Intent] =
      JsApiUtil.call1ErrFut( shim.startActivityForResult(intent, _, _) )

    def sendResultF(args: Intent): Future[Unit] =
      JsApiUtil.call0Fut( shim.sendResult(args, _) )

  }
}


trait Filters extends js.Object {
  val filterActions: js.UndefOr[js.Array[String]] = js.undefined
  val filterCategories: js.UndefOr[js.Array[String]] = js.undefined
  val filterDataSchemes: js.UndefOr[js.Array[String]] = js.undefined
}


trait Intent extends js.Object {

  val `type`,
      `package`,
      url,
      action,
      chooser,
      data: js.UndefOr[String] = js.undefined

  val extras: js.UndefOr[js.Dictionary[js.Any]] = js.undefined

  val component: js.UndefOr[IntentComponent | String] = js.undefined

  val flags: js.UndefOr[js.Array[Int]] = js.undefined

  val requestCode: js.UndefOr[Int] = js.undefined

  /** "categories:" "{android.intent.category.LAUNCHER}" */
  val categories: js.UndefOr[String] = js.undefined

}


trait IntentComponent extends js.Object {
  val `package`, `class`: js.UndefOr[String] = js.undefined
}
