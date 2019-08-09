package io.suggest.sjs.mapbox.gl.map

import io.suggest.sjs.common.model.FromDict

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 12:37
  * Description: On-screen point model API.
  */
object Point extends FromDict {
  override type T = Point
}

@js.native
sealed trait Point extends js.Object {

  var x: Int = js.native

  var y: Int = js.native

}
