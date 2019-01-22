package cordova.plugins

import cordova.plugins.diagnostic.CordovaPluginDiagnostic

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.19 16:24
  * Description: cordova.plugins interface.
  */
@js.native
sealed trait CordovaPlugins extends js.Object {

  val diagnostic: CordovaPluginDiagnostic = js.native

}
