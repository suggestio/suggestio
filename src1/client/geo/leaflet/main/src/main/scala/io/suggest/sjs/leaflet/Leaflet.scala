package io.suggest.sjs.leaflet

import io.suggest.sjs.leaflet.control.LControl
import io.suggest.sjs.leaflet.geojson.{GeoJson, GjOptions}
import io.suggest.sjs.leaflet.layer.group.{FeatureGroup, LayerGroup}
import io.suggest.sjs.leaflet.map._
import io.suggest.sjs.leaflet.marker.icon.{Icon, IconOptions}
import io.suggest.sjs.leaflet.marker.{Marker, MarkerOptions}
import io.suggest.sjs.leaflet.path.PathOptions
import io.suggest.sjs.leaflet.path.circle.{Circle, CircleMarker, CircleMarkerOptions, CircleOptions}
import io.suggest.sjs.leaflet.path.poly._
import io.suggest.sjs.leaflet.popup.{Popup, PopupOptions}
import io.suggest.sjs.leaflet.tilelayer.{TileLayer, TlOptions}

import scala.scalajs.js
import scala.scalajs.js.`|`
import scala.scalajs.js.annotation._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 16:51
  * Description: Core interface of leaflet.
  */

@js.native
trait ILeaflet extends js.Object {

  def map(target: MapTarget, options: MapOptions = js.native): LMap = js.native

  def tileLayer(urlTemplate: String, options: TlOptions = js.native): TileLayer = js.native

  /** Geo coordinates. */
  def latLng(lat: Double, lng: Double, altitude: Double = js.native): LatLng = js.native
  def latLng(latLng: js.Array[Double] | js.Object): LatLng = js.native

  def bounds(topLeft: Point, bottomRight: Point): Bounds = js.native
  def bounds(pts: js.Array[Point]): Bounds = js.native

  def icon(options: IconOptions): Icon = js.native

  def marker(latLng: LatLng, options: MarkerOptions = js.native): Marker = js.native

  /** Pixel point. */
  def point(x: Int, y: Int): Point = js.native

  /** Controls. */
  def control: LControl = js.native

  def polyline(latLng: LatLng, options: PolylineOptions = js.native): Polyline = js.native

  def polygon(latLngs: PolygonLatLngs_t, options: PolylineOptions = js.native): Polygon = js.native

  def multiPolyline(latLngs: js.Array[js.Array[LatLng]], options: PolylineOptions): MultiPolyline = js.native

  def rectangle(bounds: LatLngBounds, options: PathOptions = js.native): Rectangle = js.native

  /** Instantiate a circle. */
  @deprecated("Use circle() instead", "1.0.0")
  @JSName("circle")
  def circleApi07(latLng: LatLng, radiusMeters: Double, pathOptions: PathOptions = js.native): Circle = js.native
  def circle(latLng: LatLng, opts: CircleOptions = js.native): Circle = js.native

  def circleMarker(latLng: LatLng, options: CircleMarkerOptions = js.native): CircleMarker = js.native

  /** Instantiate a popup. */
  def popup(options: PopupOptions = js.native, source: Layer = js.native): Popup = js.native

  /** Instantiate new layer group. */
  def layerGroup(layers: js.Array[Layer] = js.native): LayerGroup = js.native

  /** Instantiate new feature group. */
  def featureGroup(layers: js.Array[Layer] = js.native): FeatureGroup = js.native

  /** Instantiate GeoJSON layers builder. */
  def geoJson(data: js.Any = js.native, options: GjOptions = js.native): GeoJson = js.native

  /** Rollback window.L to previous value. */
  def noConflict(): Leaflet.type | js.Any = js.native

  val version: String = js.native

}


@js.native
@JSImport(LEAFLET_IMPORT, JSImport.Namespace)
object Leaflet extends ILeaflet
