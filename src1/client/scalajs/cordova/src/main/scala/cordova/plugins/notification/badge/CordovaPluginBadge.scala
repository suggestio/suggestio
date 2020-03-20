package cordova.plugins.notification.badge

import io.suggest.sjs.JsApiUtil

import scala.concurrent.Future
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.03.2020 23:11
  * Description: JS API for cordova-plugin-badge.
  * @see [[https://github.com/katzer/cordova-plugin-badge#set-the-badge-number]]
  */
@js.native
trait CordovaPluginBadge extends js.Object {

  def set(badge: Int): Unit = js.native

  def increase(by: Int, onComplete: js.Function1[Int, Unit] = js.native): Unit = js.native

  def decrease(by: Int, onComplete: js.Function1[Int, Unit] = js.native): Unit = js.native

  def clear(): Unit = js.native

  def get(onComplete: js.Function1[Int, Unit]): Unit = js.native


  def configure(config: BadgePluginConfig): Unit = js.native

  def requestPermission(onComplete: js.Function1[Boolean, Unit]): Unit = js.native

  def hasPermission(onComplete: js.Function1[Boolean, Unit]): Unit = js.native

}


object CordovaPluginBadge {

  /** Scala API для сокрытия callback'ов через Future[]. */
  implicit final class CpbOpsExt( private val plugin: CordovaPluginBadge ) extends AnyVal {

    def increaseF(by: Int): Future[Int] =
      JsApiUtil.call1Fut[Int]( plugin.increase(by, _) )

    def decreaseF(by: Int): Future[Int] =
      JsApiUtil.call1Fut[Int]( plugin.decrease(by, _) )

    def getF(): Future[Int] =
      JsApiUtil.call1Fut[Int]( plugin.get )

    def requestPermissionF(): Future[Boolean] =
      JsApiUtil.call1Fut[Boolean]( plugin.requestPermission )

    def hasPermissionF(): Future[Boolean] =
      JsApiUtil.call1Fut[Boolean]( plugin.hasPermission )

  }

}
