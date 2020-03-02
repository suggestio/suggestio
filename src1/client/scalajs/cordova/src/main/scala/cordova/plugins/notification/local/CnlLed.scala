package cordova.plugins.notification.local

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.03.2020 10:28
  * @see [[https://github.com/katzer/cordova-plugin-local-notifications#properties]]
  */

trait CnlLed extends js.Object {
  val color: js.UndefOr[String] = js.undefined
  val on: js.UndefOr[Int] = js.undefined
  val off: js.UndefOr[Int] = js.undefined
}
