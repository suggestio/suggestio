package io.suggest.sjs.leaflet.control.zoom

import io.suggest.sjs.leaflet.LEAFLET_IMPORT

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 18:54
 * Description: API for zoom options model.
 */
@js.native
@JSImport(LEAFLET_IMPORT, "Control.Zoom")
class Zoom extends js.Object

@js.native
trait ZoomOptions extends js.Object {

  var animate: Boolean = js.native

}
