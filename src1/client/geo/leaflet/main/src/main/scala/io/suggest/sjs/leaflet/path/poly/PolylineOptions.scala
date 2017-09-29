package io.suggest.sjs.leaflet.path.poly

import io.suggest.sjs.leaflet.path.PathOptions

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 23:11
 * Description: Polyline constructor options.
 */
trait PolylineOptions extends PathOptions {

  val smoothFactor  : UndefOr[Double]   = js.undefined

  val noClip        : UndefOr[Boolean]  = js.undefined

}
