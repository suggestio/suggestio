package io.suggest.sjs.leaflet.map

import io.suggest.sjs.leaflet.LEAFLET_IMPORT

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 18:58
  * Description: L.Point model.
  */
@JSImport(LEAFLET_IMPORT, "Point")
@js.native
class Point extends js.Object {

  var x: Int = js.native

  var y: Int = js.native

}
