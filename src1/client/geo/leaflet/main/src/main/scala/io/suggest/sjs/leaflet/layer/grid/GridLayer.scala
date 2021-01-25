package io.suggest.sjs.leaflet.layer.grid

import io.suggest.sjs.leaflet.LEAFLET_IMPORT
import io.suggest.sjs.leaflet.layer.LayerOptions
import io.suggest.sjs.leaflet.map.{LatLngBounds, Layer, Point, Zoom_t}
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.01.2021 16:47
  * @see [[https://leafletjs.com/reference-1.6.0.html#gridlayer]]
  */
@JSImport(LEAFLET_IMPORT, "GridLayer")
@js.native
class GridLayer extends Layer {

  def bringToFront(): this.type = js.native
  def bringToBack(): this.type = js.native
  def getContainer(): dom.html.Element = js.native
  def setOpacity(opacity: Double): this.type = js.native
  def setZIndex(zIndex: Int): this.type = js.native
  def isLoading(): Boolean = js.native
  def redraw(): this.type = js.native
  def getTileSize(): Point = js.native

}


trait GridLayerOptions extends LayerOptions {

  val tileSize            : js.UndefOr[Double]         = js.undefined
  val opacity             : js.UndefOr[Double]         = js.undefined
  val updateWhenIdle      : js.UndefOr[Boolean]        = js.undefined
  val updateWhenZooming   : js.UndefOr[Boolean]        = js.undefined
  val updateInterval      : js.UndefOr[Int]            = js.undefined
  val zIndex              : js.UndefOr[Double]         = js.undefined
  val bounds              : js.UndefOr[LatLngBounds]   = js.undefined
  val minZoom             : js.UndefOr[Zoom_t] = js.undefined
  val maxZoom             : js.UndefOr[Zoom_t] = js.undefined
  val maxNativeZoom       : js.UndefOr[Zoom_t]         = js.undefined
  val minNativeZoom       : js.UndefOr[Zoom_t]         = js.undefined
  val noWrap              : js.UndefOr[Boolean]        = js.undefined
  val className           : js.UndefOr[String] = js.undefined
  val keepBuffer          : js.UndefOr[String] = js.undefined

}
