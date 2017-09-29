package io.suggest.sjs.leaflet.path.circle

import io.suggest.sjs.leaflet.path.PathOptions

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 23:04
  * Description: Extended PathOptions with radius in pixels.
  */

trait CircleMarkerOptions extends PathOptions {

  /** Radius in css-pixels. [10 px] */
  val radius    : UndefOr[Double]   = js.undefined

}
