package cordova.plugins.appminimize

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.06.2020 22:36
  * Description: SJS API for cordova-plugin-appminimize.
  * @see [[https://github.com/tomloprod/cordova-plugin-appminimize]]
  */
@js.native
@JSGlobal("window.plugins.appMinimize")
object CdvAppMinimize extends js.Object {

  def minimize(): Unit = js.native

}
