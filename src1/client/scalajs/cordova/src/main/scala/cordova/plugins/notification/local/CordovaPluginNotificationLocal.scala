package cordova.plugins.notification.local

import io.suggest.sjs.JsApiUtil

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.|
import js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.02.2020 18:48
  * Description: API для cordova-plugin-local-notifications
  * @see [[https://github.com/katzer/cordova-plugin-local-notifications/blob/example-x/www/js/index.js]]
  */
@js.native
trait CordovaPluginNotificationLocal extends js.Object {

  // Трудно понять точный формат, но видимо там object
  // https://github.com/katzer/cordova-plugin-local-notifications/blob/example-x/www/js/index.js#L83
  val launchDetails: js.UndefOr[js.Object] = js.native

  def hasPermission(cb: js.Function1[Boolean, Unit]): Unit = js.native

  def requestPermission(cb: js.Function1[Boolean, Unit]): Unit = js.native

  def schedule(
                toasts: CnlToast | js.Array[CnlToast],
                cb: js.Function0[Unit] = js.native,
                scope: js.Any = js.native,
                opts: js.UndefOr[CnlSheduleOpts] = js.native,
              ): Unit = js.native

  def addActions(name: String, actions: js.Array[CnlAction]): Unit = js.native

  def update(notification: CnlToast): Unit = js.native

  def on(event: String,
         callback: js.Function,
         scope: js.Any = js.native
        ): Unit = js.native

  def un(event: String,
         callback: js.Function,
        ): Unit = js.native

  val _listener: js.Dictionary[js.Array[js.Function]]

  val core: CnlCore = js.native

  /** @see [[https://github.com/katzer/cordova-plugin-local-notifications#launch-details]] */
  def fireQueuedEvents(): Unit = js.native

  def clear( id: Int | js.Array[Int],
             onComplete: js.Function0[Unit] = js.native ): Unit = js.native

  def clearAll(onComplete: js.Function0[Unit] = js.native): Unit = js.native

  def cancel( id: Int | js.Array[Int],
              onComplete: js.Function0[Unit] = js.native ): Unit = js.native

  def cancelAll(onComplete: js.Function0[Unit] = js.native): Unit = js.native

  // notification is scheduled or triggered
  def isPresent(id: Int,
                callback: js.Function1[Boolean, Unit] ): Unit = js.native

  def isScheduled(id: Int,
                  callback: js.Function1[Boolean, Unit]): Unit = js.native

  def isTriggered(id: Int,
                  callback: js.Function1[Boolean, Unit]): Unit = js.native

  def getType(id: Int,
              callback: js.Function1[String, Unit]): Unit = js.native

  def getScheduledIds( callback: js.Function1[js.Array[Int], Unit] ): Unit = js.native

  def getTriggeredIds( callback: js.Function1[js.Array[Int], Unit] ): Unit = js.native

  def getScheduled( callback: js.Function1[js.Array[CnlToast], Unit] ): Unit = js.native

  def getTriggered( callback: js.Function1[js.Array[CnlToast], Unit] ): Unit = js.native

  def get(id: Int, callback: js.Function1[js.UndefOr[CnlToast], Unit]): Unit = js.native
  def get(ids: js.Array[Int], callback: js.Function1[js.Array[CnlToast], Unit]): Unit = js.native

  def getAll( callback: js.Function1[js.Array[CnlToast], Unit] ): Unit = js.native

  def setDefaults(notification: CnlToast): Unit = js.native

}

object CordovaPluginNotificationLocal {

  /** scala API, для callback-методов, которые заменены на Future. */
  implicit class CnlOpsExt( private val cnl: CordovaPluginNotificationLocal ) extends AnyVal {

    def scheduleF(
                   toasts: CnlToast | js.Array[CnlToast],
                   scope: js.Any = js.undefined,
                   opts: js.UndefOr[CnlSheduleOpts] = js.undefined,
                 ): Future[_] = {
      JsApiUtil.call0Fut( cnl.schedule(toasts, _, scope, opts) )
    }

    def hasPermissionF(): Future[Boolean] =
      JsApiUtil.call1Fut[Boolean]( cnl.hasPermission )

    def requestPermissionF(): Future[Boolean] =
      JsApiUtil.call1Fut[Boolean]( cnl.requestPermission )

    def clearF(ids: Int*): Future[Unit] = {
      JsApiUtil.call0Fut( jsCbF =>
        cnl.clear( ids.toJSArray, jsCbF )
      )
    }

    def clearAllF(): Future[Unit] =
      JsApiUtil.call0Fut( cnl.clearAll )


    def cancelF(ids: Int*): Future[Unit] = {
      JsApiUtil.call0Fut( jsCbF =>
        cnl.cancel( ids.toJSArray, jsCbF )
      )
    }

    def cancelAllF(): Future[Unit] =
      JsApiUtil.call0Fut( cnl.cancelAll )


    def isPresentF(id: Int): Future[Boolean] =
      JsApiUtil.call1Fut[Boolean]( cnl.isPresent(id, _) )

    def isScheduledF(id: Int): Future[Boolean] =
      JsApiUtil.call1Fut[Boolean]( cnl.isScheduled(id, _) )

    def isTriggeredF(id: Int): Future[Boolean] =
      JsApiUtil.call1Fut[Boolean]( cnl.isTriggered(id, _) )

    def getTypeF(id: Int): Future[String] =
      JsApiUtil.call1Fut[String]( cnl.getType(id, _) )

    def getScheduledIdsF(): Future[js.Array[Int]] =
      JsApiUtil.call1Fut[js.Array[Int]]( cnl.getScheduledIds )

    def getTriggeredIdsF(): Future[js.Array[Int]] =
      JsApiUtil.call1Fut[js.Array[Int]]( cnl.getTriggeredIds )

    def getScheduledF(): Future[js.Array[CnlToast]] =
      JsApiUtil.call1Fut[js.Array[CnlToast]]( cnl.getScheduled )

    def getTriggeredF(): Future[js.Array[CnlToast]] =
      JsApiUtil.call1Fut[js.Array[CnlToast]]( cnl.getTriggered )

    def getF(ids: Int*): Future[js.Array[CnlToast]] = {
      JsApiUtil.call1Fut[js.Array[CnlToast]] {
        cnl.get( ids.toJSArray, _ )
      }
    }

    def getAllF(): Future[js.Array[CnlToast]] =
      JsApiUtil.call1Fut[js.Array[CnlToast]]( cnl.getAll )

  }

}


@js.native
sealed trait CnlCore extends js.Object {
  def fireEvent(event: String, args: js.Any): Unit = js.native
}
