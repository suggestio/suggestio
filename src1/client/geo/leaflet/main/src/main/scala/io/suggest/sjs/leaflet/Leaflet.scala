package io.suggest.sjs.leaflet

import io.suggest.sjs.leaflet.control.LControl
import io.suggest.sjs.leaflet.geojson.{GeoJson, GjOptions}
import io.suggest.sjs.leaflet.layer.group.{FeatureGroup, LayerGroup}
import io.suggest.sjs.leaflet.map._
import io.suggest.sjs.leaflet.marker.icon.{Icon, IconOptions}
import io.suggest.sjs.leaflet.marker.{Marker, MarkerOptions}
import io.suggest.sjs.leaflet.path.PathOptions
import io.suggest.sjs.leaflet.path.circle.{Circle, CircleMarker, CircleMarkerOptions}
import io.suggest.sjs.leaflet.path.poly._
import io.suggest.sjs.leaflet.popup.{Popup, PopupOptions}
import io.suggest.sjs.leaflet.tilelayer.{TileLayer, TlOptions}

import scala.scalajs.js
import scala.scalajs.js.`|`
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 16:51
  * Description: Core interface of leaflet.
  */
@JSImport("leaflet", JSImport.Namespace)
@js.native
object Leaflet extends js.Object {

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

  def polygon(latLngs: js.Array[LatLng | js.Array[LatLng]], options: PolylineOptions): Polygon = js.native

  def multiPolyline(latLngs: js.Array[js.Array[LatLng]], options: PolylineOptions): MultiPolyline = js.native

  def rectangle(bounds: LatLngBounds, options: PathOptions = js.native): Rectangle = js.native

  /** Instantiate a circle. */
  def circle(latLng: LatLng, radiusMeters: Double, pathOptions: PathOptions = js.native): Circle = js.native

  def circleMarker(latLng: LatLng, options: CircleMarkerOptions = js.native): CircleMarker = js.native

  /** Instantiate a popup. */
  def popup(options: PopupOptions = js.native, source: Layer = js.native): Popup = js.native

  /** Instantiate new layer group. */
  def layerGroup(layers: js.Array[Layer] = js.native): LayerGroup = js.native

  /** Instantiate new feature group. */
  def featureGroup(layers: js.Array[Layer] = js.native): FeatureGroup = js.native

  /** Instantiate GeoJSON layers builder. */
  def geoJson(data: js.Any = js.native, options: GjOptions = js.native): GeoJson = js.native

}
