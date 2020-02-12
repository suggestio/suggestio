package cordova

import cordova.plugins.CordovaPlugins

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

  val plugins: CordovaPlugins = js.native

  /** Имя под-директории в app/platforms/
    * @return "android" | "ios" | ...
    * @see [[https://stackoverflow.com/a/30800591]]
    */
  def platformId: String = js.native

}
