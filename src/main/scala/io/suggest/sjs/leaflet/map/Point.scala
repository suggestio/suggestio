package io.suggest.sjs.leaflet.map

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 18:58
  * Description: L.Point model.
  */
@js.native
@JSName("L.Point")
sealed trait Point extends js.Object {

  var x: Int = js.native

  var y: Int = js.native

}
