package cordova.plugins.background.geolocation

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.02.2021 12:44
  */
@js.native
trait EventSubscription extends js.Object {

  def remove(): Unit = js.native

}
