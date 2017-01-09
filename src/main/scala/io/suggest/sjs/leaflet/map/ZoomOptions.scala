package io.suggest.sjs.leaflet.map

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 18:54
 * Description: API for zoom options model.
 */
@js.native
sealed trait ZoomOptions extends js.Object {

  var animate: Boolean = js.native

}
