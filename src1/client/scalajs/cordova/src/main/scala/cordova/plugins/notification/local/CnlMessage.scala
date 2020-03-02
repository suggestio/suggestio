package cordova.plugins.notification.local

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.03.2020 10:39
  * @see [[https://github.com/katzer/cordova-plugin-local-notifications#summarizing]]
  */
trait CnlMessage extends js.Object {
  val message: String
  val person: js.UndefOr[String] = js.undefined
}
