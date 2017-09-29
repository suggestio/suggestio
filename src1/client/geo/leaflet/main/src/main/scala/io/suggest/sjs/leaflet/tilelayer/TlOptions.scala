package io.suggest.sjs.leaflet.tilelayer

import io.suggest.sjs.leaflet.map.{LatLngBounds, Zoom_t}

import scala.scalajs.js
import scala.scalajs.js.{UndefOr, `|`}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 18:04
  * Description: Interface for TileLayer options model.
  */

trait TlOptions extends js.Object {

  val minZoom             : UndefOr[Zoom_t]         = js.undefined
  val maxZoom             : UndefOr[Zoom_t]         = js.undefined
  val maxNativeZoom       : UndefOr[Zoom_t]         = js.undefined
  val tileSize            : UndefOr[Double]         = js.undefined
  val subdomains          : UndefOr[String | js.Array[String]] = js.undefined
  val errorTileUrl        : UndefOr[String]         = js.undefined
  val attribution         : UndefOr[String]         = js.undefined
  val tms                 : UndefOr[Boolean]        = js.undefined
  val continuousWorld     : UndefOr[Boolean]        = js.undefined
  val noWrap              : UndefOr[Boolean]        = js.undefined
  val zoomOffset          : UndefOr[Double]         = js.undefined
  val zoomReverse         : UndefOr[Boolean]        = js.undefined
  val opacity             : UndefOr[Double]         = js.undefined
  val zIndex              : UndefOr[Double]         = js.undefined
  val unloadInvisibleTiles: UndefOr[Boolean]        = js.undefined
  val updateWhenIdle      : UndefOr[Boolean]        = js.undefined
  val detectRetina        : UndefOr[Boolean]        = js.undefined
  val reuseTiles          : UndefOr[Boolean]        = js.undefined
  val bounds              : UndefOr[LatLngBounds]   = js.undefined

}
