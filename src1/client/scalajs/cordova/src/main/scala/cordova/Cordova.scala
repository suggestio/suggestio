package cordova

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.10.16 11:35
  * Description: API for cordova.js.
  */
@JSGlobal("cordova")
@js.native
object Cordova extends js.Object {

  def require[T](id: String): T = js.native

}
