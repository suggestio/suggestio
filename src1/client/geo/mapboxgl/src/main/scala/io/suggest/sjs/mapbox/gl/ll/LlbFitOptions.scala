package io.suggest.sjs.mapbox.gl.ll

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.mapbox.gl.Zoom_t

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 12:39
  * Description: API for map.fitToBounds() options.
  */

object LlbFitOptions extends FromDict {
  override type T = LlbFitOptions
}


@js.native
sealed trait LlbFitOptions extends js.Object {

  var linear: Boolean = js.native

  def easing(t: Double): Double = js.native

  var padding: Int = js.native

  var maxZoom: Zoom_t = js.native

}
