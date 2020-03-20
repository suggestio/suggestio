package cordova.plugins.notification

import cordova.plugins.notification.badge.CordovaPluginBadge
import cordova.plugins.notification.local.CordovaPluginNotificationLocal

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.02.2020 18:47
  */
@js.native
trait CordovaPluginNotification extends js.Object {

  val badge: CordovaPluginBadge = js.native

  val local: CordovaPluginNotificationLocal = js.native

}
