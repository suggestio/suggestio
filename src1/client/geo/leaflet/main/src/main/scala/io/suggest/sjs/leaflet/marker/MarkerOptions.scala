package io.suggest.sjs.leaflet.marker

import io.suggest.sjs.leaflet.marker.icon.Icon

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.ScalaJSDefined

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.11.15 15:47
 * Description: API for marker options.
 */

@ScalaJSDefined
trait MarkerOptions extends js.Object {

  val icon          : UndefOr[Icon]     = js.undefined

  val clickable     : UndefOr[Boolean]  = js.undefined

  val draggable     : UndefOr[Boolean]  = js.undefined

  val keyboard      : UndefOr[Boolean]  = js.undefined

  val title         : UndefOr[String]   = js.undefined

  val alt           : UndefOr[String]   = js.undefined

  val zIndexOffset  : UndefOr[Int]      = js.undefined

  val opacity       : UndefOr[Double]   = js.undefined

  val riseOnHover   : UndefOr[Boolean]  = js.undefined

  val riseOffset    : UndefOr[Int]      = js.undefined

}
