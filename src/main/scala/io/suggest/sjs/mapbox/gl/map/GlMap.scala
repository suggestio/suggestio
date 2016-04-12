package io.suggest.sjs.mapbox.gl.map

import io.suggest.sjs.mapbox.gl.{Bearing_t, Pitch_t, Zoom_t}
import io.suggest.sjs.mapbox.gl.anim.AnimationOptions
import io.suggest.sjs.mapbox.gl.camera.CameraOptions
import io.suggest.sjs.mapbox.gl.control.Control
import io.suggest.sjs.mapbox.gl.err.MbglError
import io.suggest.sjs.mapbox.gl.event.{EventData, Evented}
import io.suggest.sjs.mapbox.gl.layer.{ILayer, LayerFeature}
import io.suggest.sjs.mapbox.gl.ll.{LlbFitOptions, LngLat, LngLatBounds}
import io.suggest.sjs.mapbox.gl.style.{Style, StyleBatch, StyleOptions}
import io.suggest.sjs.mapbox.gl.style.source.Source
import org.scalajs.dom.raw.{HTMLCanvasElement, HTMLElement}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.{UndefOr, |}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 11:14
  * Description: mapboxgl.Map class API.
  */

@js.native
@JSName("mapboxgl.Map")
class GlMap(options: GlMapOptions) extends StyleBatch with Evented {

  def addClass(klass: String, options: StyleOptions = js.native): this.type = js.native

  def addControl(control: Control): this.type = js.native

  def batch(f: js.Function1[StyleBatch, _]): Unit = js.native

  def easeTo(options: CameraOptions | AnimationOptions, eventData: EventData = js.native): this.type = js.native

  def fitBounds(bounds: LngLatBounds, options: LlbFitOptions, eventData: EventData = js.native): this.type = js.native

  def flyTo(options: FlyOptions, eventData: EventData = js.native): this.type = js.native

  def getBearing(): Bearing_t = js.native

  def getBounds(): LngLatBounds = js.native

  def getCanvas(): HTMLCanvasElement = js.native
  def getCanvasContainer(): HTMLElement = js.native

  def getCenter(): LngLat = js.native

  def getClasses(): js.Array[String] = js.native

  def getContainer(): HTMLElement = js.native

  def getFilter(layerId: String): js.Array[js.Object] = js.native
  def getLayer(layerId: String): ILayer = js.native
  def getLayoutProperty(layerId: String, propName: String, klass: String = js.native): js.Any = js.native
  def getPaintProperty(layerId: String, propName: String, klass: String = js.native): js.Any = js.native

  def getPitch(): Pitch_t = js.native

  def getSource(id: String): Source = js.native
  def getStyle(): Style = js.native

  def getZoom(): Zoom_t = js.native

  def hasClass(klass: String): Boolean = js.native

  def jumpTo(options: CameraOptions, eventData: EventData = js.native): this.type = js.native

  def loaded(): Boolean = js.native

  var onError: js.Function1[MbglError, _] = js.native

  def panBy(offset: Point | js.Array[Double],
            options: AnimationOptions = js.native,
            eventData: EventData = js.native): this.type = js.native
  def panTo(lngLat: LngLat,
            options: AnimationOptions = js.native,
            eventData: EventData = js.native): this.type = js.native
  def project(lngLat: LngLat): Point = js.native

  def queryRenderedFeatures(pointOrBox: js.Array[Double] | js.Array[js.Array[Double]] = js.native,
                            params: QueryRenderedOptions): js.Array[LayerFeature] = js.native

  def querySourceFeatures(sourceId: String, params: QuerySourceOptions): js.Array[LayerFeature] = js.native

  def remove(): UndefOr[_] = js.native
  def removeClass(klass: String, options: StyleOptions = js.native): this.type = js.native

  var repaint: Boolean = js.native

  def resetNorth(options: AnimationOptions = js.native, eventData: EventData = js.native): this.type = js.native

  def resize(): this.type = js.native
  def rotateTo(bearing: Bearing_t,
               options: AnimationOptions = js.native,
               eventData: EventData = js.native): this.type = js.native

  def setBearing(bearing: Bearing_t, eventData: EventData = js.native): this.type = js.native

  def setCenter(center: LngLat, eventData: EventData = js.native): this.type = js.native

  def setClasses(klasses: js.Array[String], options: StyleOptions = js.native): this.type = js.native

  def setMaxBounds(llb: LngLatBounds | js.Array[js.Array[Double]] = js.native): this.type = js.native

  def setMaxZoom(maxZoom: Zoom_t): this.type = js.native
  def setMinZoom(minZoom: Zoom_t): this.type = js.native

  def setPitch(pitch: Pitch_t, eventData: EventData = js.native): this.type = js.native

  def setStyle(style: Style): this.type = js.native

  def setZoom(zoom: Zoom_t, eventData: EventData = js.native): this.type = js.native

  var showCollisionBoxes: Boolean = js.native
  var showTileBoundaries: Boolean = js.native

  def snapToNorth(options: AnimationOptions, eventData: EventData = js.native): this.type = js.native

  def stop(): this.type = js.native

  def unproject(point: Point | js.Array[Double]): LngLat = js.native

  def zoomIn(options: AnimationOptions = js.native, eventData: EventData = js.native): this.type = js.native
  def zoomOut(options: AnimationOptions = js.native, eventData: EventData = js.native): this.type = js.native

  def zoomTo(zoom: Zoom_t, options: AnimationOptions = js.native, eventData: EventData = js.native): this.type = js.native

}
