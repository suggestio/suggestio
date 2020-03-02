package cordova.plugins.notification.local

import scala.concurrent.{Future, Promise}
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

  def schedule(notification: CnlNotification): Unit = js.native
  def schedule(notifications: js.Array[CnlNotification]): Unit = js.native

  def addActions(name: String, actions: js.Array[CnlAction]): Unit = js.native

  def update(notification: CnlNotification): Unit = js.native

  def on(event: String,
         callback: js.Function2[CnlNotification, CnlEventOpts, Unit],
         scope: js.Any = js.native
        ): Unit = js.native
  def un(event: String,
         callback: js.Function2[CnlNotification, CnlEventOpts, Unit],
         scope: js.Any = js.native
        ): Unit = js.native

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

  def getScheduled( callback: js.Function1[js.Array[CnlNotification], Unit] ): Unit = js.native

  def getTriggered( callback: js.Function1[js.Array[CnlNotification], Unit] ): Unit = js.native

  def get(id: Int | js.Array[Int], callback: js.Function1[js.UndefOr[CnlNotification], Unit]): Unit = js.native

  def getAll( callback: js.Function1[js.Array[CnlNotification], Unit] ): Unit = js.native

  def setDefaults(notification: CnlNotification): Unit = js.native

}

object CordovaPluginNotificationLocal {

  /** scala API, для callback-методов, которые заменены на Future. */
  implicit class CnlOpsExt( private val cnl: CordovaPluginNotificationLocal ) extends AnyVal {

    def hasPermissionF(): Future[Boolean] = {
      val p = Promise[Boolean]()
      cnl.hasPermission( p.success )
      p.future
    }

    def requestPermissionF(): Future[Boolean] = {
      val p = Promise[Boolean]()
      cnl.requestPermission( p.success )
      p.future
    }

    def clearF(ids: Int*): Future[Unit] = {
      val p = Promise[Unit]()
      cnl.clear(
        ids.toJSArray,
        () => p.success(),
      )
      p.future
    }

    def clearAllF(): Future[Unit] = {
      val p = Promise[Unit]()
      cnl.clearAll(
        () => p.success(),
      )
      p.future
    }


    def cancelF(ids: Int*): Future[Unit] = {
      val p = Promise[Unit]()
      cnl.cancel(
        ids.toJSArray,
        () => p.success(),
      )
      p.future
    }

    def cancelAllF(): Future[Unit] = {
      val p = Promise[Unit]()
      cnl.cancelAll(
        () => p.success(),
      )
      p.future
    }


    def isPresentF(id: Int): Future[Boolean] = {
      val p = Promise[Boolean]()
      cnl.isPresent( id, p.success )
      p.future
    }

    def isScheduledF(id: Int): Future[Boolean] = {
      val p = Promise[Boolean]()
      cnl.isScheduled(id, p.success)
      p.future
    }

    def isTriggeredF(id: Int): Future[Boolean] = {
      val p = Promise[Boolean]()
      cnl.isTriggered(id, p.success)
      p.future
    }

    def getTypeF(id: Int): Future[String] = {
      val p = Promise[String]()
      cnl.getType(id, p.success)
      p.future
    }

    def getScheduledIdsF(): Future[js.Array[Int]] = {
      val p = Promise[js.Array[Int]]()
      cnl.getScheduledIds( p.success )
      p.future
    }

    def getTriggeredIdsF(): Future[js.Array[Int]] = {
      val p = Promise[js.Array[Int]]()
      cnl.getTriggeredIds( p.success )
      p.future
    }

    def getScheduledF(): Future[js.Array[CnlNotification]] = {
      val p = Promise[js.Array[CnlNotification]]()
      cnl.getScheduled( p.success )
      p.future
    }

    def getTriggeredF(): Future[js.Array[CnlNotification]] = {
      val p = Promise[js.Array[CnlNotification]]()
      cnl.getTriggered( p.success )
      p.future
    }

    def getF(ids: Int*): Future[Option[CnlNotification]] = {
      val p = Promise[Option[CnlNotification]]()
      cnl.get(
        ids.toJSArray,
        ntfUndef =>
          p success ntfUndef.toOption,
      )
      p.future
    }

    def getAllF(): Future[js.Array[CnlNotification]] = {
      val p = Promise[js.Array[CnlNotification]]()
      cnl.getAll( p.success )
      p.future
    }

  }

}


@js.native
sealed trait CnlCore extends js.Object {
  def fireEvent(event: String, args: js.Any): Unit = js.native
}
