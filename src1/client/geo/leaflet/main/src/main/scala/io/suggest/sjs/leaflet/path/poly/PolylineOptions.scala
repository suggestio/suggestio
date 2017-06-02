package io.suggest.sjs.leaflet.path.poly

import io.suggest.sjs.leaflet.path.PathOptions

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.ScalaJSDefined

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 23:11
 * Description: Polyline constructor options.
 */
@ScalaJSDefined
trait PolylineOptions extends PathOptions {

  val smoothFactor  : UndefOr[Double]   = js.undefined

  val noClip        : UndefOr[Boolean]  = js.undefined

}
