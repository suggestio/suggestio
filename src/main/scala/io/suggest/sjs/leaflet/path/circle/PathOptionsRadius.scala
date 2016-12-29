package io.suggest.sjs.leaflet.path.circle

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.leaflet.path.PathOptions

import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 23:04
  * Description: Extended PathOptions with radius in pixels.
  */
object PathOptionsRadius extends FromDict {
  override type T = PathOptionsRadius
}


@ScalaJSDefined
trait PathOptionsRadius extends PathOptions {

  /** Radius in css-pixels. */
  val radius: Double

}
