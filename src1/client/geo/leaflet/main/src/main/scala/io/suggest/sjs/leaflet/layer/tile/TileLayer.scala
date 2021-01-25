package io.suggest.sjs.leaflet.layer.tile

import io.suggest.sjs.leaflet.LEAFLET_IMPORT
import io.suggest.sjs.leaflet.layer.grid.{GridLayer, GridLayerOptions}
import io.suggest.sjs.leaflet.map.{LMap, Zoom_t}

import scala.scalajs.js
import scala.scalajs.js.|
import scala.scalajs.js.annotation.JSImport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 18:26
 * Description: API for L.TileLayer model.
 */
@JSImport(LEAFLET_IMPORT, "TileLayer")
@js.native
class TileLayer extends GridLayer {

  def addTo(map: LMap): TileLayer = js.native

  def setZIndex(zIndex: Double): TileLayer = js.native

  def setUrl(urlTemplate: String): TileLayer = js.native

}


trait TileLayerOptions extends GridLayerOptions {

  val subdomains          : js.UndefOr[String | js.Array[String]] = js.undefined
  val errorTileUrl        : js.UndefOr[String]         = js.undefined
  val zoomOffset          : js.UndefOr[Double]         = js.undefined
  val tms                 : js.UndefOr[Boolean]        = js.undefined
  val zoomReverse         : js.UndefOr[Boolean]        = js.undefined
  val detectRetina        : js.UndefOr[Boolean]        = js.undefined
  val crossOrigin         : js.UndefOr[Boolean | String] = js.undefined

}