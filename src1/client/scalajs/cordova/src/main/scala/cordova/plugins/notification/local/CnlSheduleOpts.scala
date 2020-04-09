package cordova.plugins.notification.local

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.03.2020 13:01
  * Description: 4th argument of schedule().
  */
trait CnlSheduleOpts extends js.Object {

  val skipPermission: js.UndefOr[Boolean] = js.undefined

}
