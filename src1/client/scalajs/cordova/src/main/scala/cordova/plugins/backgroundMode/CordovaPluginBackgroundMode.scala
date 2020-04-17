package cordova.plugins.backgroundMode

import io.suggest.sjs.JsApiUtil

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.04.2020 17:56
  * Description: API for cordova.plugins.backgroundMode .
  */
@js.native
trait CordovaPluginBackgroundMode extends js.Object {

  def enable(): Unit = js.native
  def disable(): Unit = js.native
  def setEnabled(isEnabled: Boolean): Unit = js.native

  def isActive(): Boolean = js.native
  def isEnabled(): Boolean = js.native

  def on(eventType: String, f: js.Function0[Unit]): Unit = js.native
  def un(eventType: String, f: js.Function): Unit = js.native

  def moveToBackground(): Unit = js.native
  def moveToForeground(): Unit = js.native

  def overrideBackButton(): Unit = js.native
  def excludeFromTaskList(): Unit = js.native

  def isScreenOff(cb: js.Function1[Boolean, Unit]): Unit = js.native

  def wakeUp(): Unit = js.native
  def unlock(): Unit = js.native

  /** To indicate that the app is executing tasks in background: */
  def setDefaults(defaults: CbgmDefaults): Unit = js.native
  /** To modify the currently displayed notification: */
  def configure(defaults: CbgmDefaults): Unit = js.native
  def getDefaults(): CbgmDefaults = js.native

  def getSettings(): js.Object = js.native

  def disableWebViewOptimizations(): Unit = js.native
  def disableBatteryOptimizations(): Unit = js.native

  def openAppStartSettings(alert: Boolean | js.Object): Unit = js.native

  def fireEvent(event: js.Object): Unit = js.native

}


object CordovaPluginBackgroundMode {

  /** Scala API поверх некоторых исходных методов. */
  implicit final class CbmOpsExt( private val cbm: CordovaPluginBackgroundMode ) extends AnyVal {

    @inline def onF(eventType: String)(cb: () => Unit): Unit =
      cbm.on(eventType, cb)

    @inline def unF(eventType: String)(cb: () => Unit): Unit =
      cbm.un(eventType, cb)

    def isScreenOffF(): Future[Boolean] =
      JsApiUtil.call1Fut[Boolean]( cbm.isScreenOff )

  }

}
