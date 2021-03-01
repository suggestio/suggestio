package io.suggest.sjs.leaflet.map

import io.suggest.sjs.leaflet.LEAFLET_IMPORT
import io.suggest.sjs.leaflet.control.Control
import io.suggest.sjs.leaflet.control.zoom.ZoomOptions
import io.suggest.sjs.leaflet.event.{Evented, MouseEvent}
import io.suggest.sjs.leaflet.handler.IHandler
import io.suggest.sjs.leaflet.layer.group.ControlledLayer
import io.suggest.sjs.leaflet.popup.{Popup, PopupOptions}
import org.scalajs.dom.raw.{HTMLElement, Position}
import org.scalajs.dom.PositionError

import scala.scalajs.js
import scala.scalajs.js.`|`
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 16:52
  * Description: API for L.map() instance.
  * @see [[http://leafletjs.com/reference.html#map-set-methods]]
  */
@js.native
@JSImport(LEAFLET_IMPORT, "Map")
class LMap extends Evented with ControlledLayer {

  def setView(center: LatLng, zoom: Zoom_t = js.native, zoomOpts: ZoomOptions = js.native): LMap = js.native

  def setZoom(zoom: Zoom_t, zoopOpts: ZoomOptions = js.native): LMap = js.native
  def zoomIn(delta: Double, zoomOpts: ZoomOptions = js.native): LMap = js.native
  def zoomOut(delta: Double, zoomOpts: ZoomOptions = js.native): LMap = js.native
  def setZoomAround(aroundThis: LatLng | Point, zoom: Zoom_t, zoomOpts: ZoomOptions = js.native): LMap = js.native

  def fitBounds(latLngBounds: LatLngBounds, options: FitBoundsOptions = js.native): LMap = js.native

  def fitWorld(options: FitBoundsOptions = js.native): LMap = js.native

  def panTo(latLng: LatLng, options: PanOptions = js.native): LMap = js.native
  def panBy(point: Point, options: PanOptions = js.native): LMap = js.native

  def flyTo(latLng: LatLng, zoom: Zoom_t = js.native, options: ZoomOptions | PanOptions = js.native): LMap = js.native
  def flyToBounds(bounds: LatLngBounds, options: FitBoundsOptions = js.native): LMap = js.native

  def invalidateSize(animate: Boolean): LMap = js.native
  def invalidateSize(options: PanOptions): LMap = js.native

  def setMaxBounds(bound: LatLngBounds): LMap = js.native
  def setMinZoom(zoom: Zoom_t): LMap = js.native
  def setMaxZoom(zoom: Zoom_t): LMap = js.native
  def panInsideBounds(bounds: LatLngBounds, options: PanOptions = js.native): LMap = js.native
  def panInside(latLng: LatLng, options: js.Object /*FitBoundsOptions*/ = js.native): LMap = js.native
  def stop(): LMap = js.native

  def locate(options: LocateOptions): LMap = js.native
  def stopLocate(): LMap = js.native
  var _locateOptions: js.UndefOr[LocateOptions] = js.native
  def _handleGeolocationResponse(pos: Position): Unit = js.native
  def _handleGeolocationError(error: PositionError): Unit = js.native

  def remove(): LMap = js.native


  // Methods for Getting Map State
  // http://leafletjs.com/reference.html#map-get-methods

  def getCenter(): LatLng = js.native

  // 2020-06-23 Внезапно, getZoom() вернула бесконечную дробь. Поэтому тут Double.
  def getZoom(): Double   = js.native
  def getMinZoom(): Zoom_t = js.native
  def getMaxZoom(): Zoom_t = js.native

  def getBounds(): LatLngBounds = js.native
  def getBoundsZoom(bound: LatLngBounds, inside: Boolean = js.native): Double = js.native
  def getSize(): Point = js.native

  def getPixelBounds(): Bounds = js.native
  def getPixelOrigin(): Point = js.native


  // Methods for Layers and Controls
  // http://leafletjs.com/reference.html#map-stuff-methods
  def hasLayer(layer: Layer): Boolean = js.native
  def eachLayer(f: js.Function1[Layer,_], ctx: js.Object = js.native): this.type = js.native

  def openPopup(popup: Popup): this.type = js.native
  def openPopup(html: String | HTMLElement, latLng: LatLng, options: PopupOptions = js.native): this.type = js.native
  def closePopup(popup: Popup = js.native): this.type = js.native

  def addControl(control: Control): this.type = js.native
  def removeControl(control: Control): this.type = js.native


  // Conversion Methods
  // http://leafletjs.com/reference.html#map-conversion-methods
  def latLngToLayerPoint(latLng: LatLng): Point = js.native
  def layerPointToLatLng(point: Point): LatLng  = js.native

  def containerPointToLayerPoint(point: Point): Point = js.native
  def layerPointToContainerPoint(point: Point): Point = js.native

  def latLngToContainerPoint(latLng: LatLng): Point = js.native
  def containerPointToLatLng(point: Point): LatLng  = js.native

  def project(latLng: LatLng, zoom: Zoom_t = js.native): Point = js.native
  def unproject(point: Point, zoom: Zoom_t = js.native): LatLng = js.native

  def mouseEventToContainerPoint(event: MouseEvent): Point = js.native
  def mouseEventToLayerPoint(event: MouseEvent): Point = js.native
  def mouseEventToLatLng(event: MouseEvent): LatLng = js.native


  // other methods
  // http://leafletjs.com/reference.html#map-misc-methods
  def getContainer(): HTMLElement = js.native
  def getPanes(): MapPanes = js.native
  def whenReady(f: js.Function0[_], ctx: js.Object = js.native): this.type = js.native


  var dragging: IHandler = js.native
  var touchZoom: IHandler = js.native
  var doubleClickZoom: IHandler = js.native
  var scrollWheelZoom: IHandler = js.native
  var boxZoom: IHandler = js.native
  var keyboard: IHandler = js.native
  var tap: IHandler = js.native

  // private api
  var _sizeChanged: js.UndefOr[Boolean] = js.native

  // TODO zoomControl, attributionControl.

}
