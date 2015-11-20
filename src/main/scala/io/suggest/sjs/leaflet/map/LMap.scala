package io.suggest.sjs.leaflet.map

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 16:52
  * Description: API for L.map() instance.
  */
@js.native
@JSName("L.Map")
class LMap extends js.Object {

  def setView(center: LatLng, zoom: Zoom_t = js.native, zoomOpts: ZoomOptions = js.native): LMap = js.native


  def setZoom(zoom: Zoom_t, zoopOpts: ZoomOptions = js.native): LMap = js.native

  def zoomIn(delta: Double, zoomOpts: ZoomOptions = js.native): LMap = js.native

  def zoomOut(delta: Double, zoomOpts: ZoomOptions = js.native): LMap = js.native

  def setZoomAround(latLng: LatLng, zoom: Zoom_t, zoomOpts: ZoomOptions = js.native): LMap = js.native


  def fitBounds(latLngBounds: LatLngBounds, options: FitBoundsOptions = js.native): LMap = js.native

  def fitWorld(options: FitBoundsOptions = js.native): LMap = js.native

  def panTo(latLng: LatLng, options: PanOptions = js.native): LMap = js.native

  def invalidateSize(animate: Boolean): LMap = js.native
  def invalidateSize(options: PanOptions): LMap = js.native

  def setMaxBounds(bound: LatLngBounds): LMap = js.native

  def locate(options: LocateOptions): LMap = js.native
  def stopLocate(): LMap = js.native

  def remove(): LMap = js.native

}
