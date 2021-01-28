package io.suggest.sjs

import io.suggest.sjs.leaflet.map.{LatLng, LatLngBounds, LatLngLiteral}
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.`|`

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 16:45
  */
package object leaflet {

  // TODO esm-модуль уже работает, но проблема с MarkerCluster, которые требует старую window.L, и не понимает imports-loader через esm-js-файл
  //final val LEAFLET_IMPORT = "leaflet/dist/leaflet-src.esm.js"
  final val LEAFLET_IMPORT = "leaflet"

  type MapTarget = String | HTMLElement

  type PolygonCoords_t = js.Array[Double] | js.Array[js.Array[Double]] | js.Array[js.Array[js.Array[Double]]] | js.Array[js.Array[js.Array[js.Array[Double]]]]

  /** @see [[https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/leaflet/index.d.ts#L135]] */
  type LatLngTuple = js.Array[Double]  // [number, number]
  /** @see [[https://github.com/DefinitelyTyped/DefinitelyTyped/blob/master/types/leaflet/index.d.ts#L137]] */
  type LatLngExpression = LatLng | LatLngLiteral | LatLngTuple

  /** Polygon bodies as GeoJSON coordinates arrays.
    *
    * Described as:
    * PropTypes.oneOfType([
    *   latlngListType,
    *   multiLatLngListType,
    *   PropTypes.arrayOf(multiLatLngListType),
    * ]).isRequired
    * @see [[http://leafletjs.com/reference-1.0.3.html#polygon]]
    * @see [[https://github.com/PaulLeCam/react-leaflet/blob/master/src/Polygon.js]]
    */
  type PolygonLatLngs_t = js.Array[LatLngExpression] | js.Array[js.Array[LatLngExpression]] | js.Array[js.Array[js.Array[LatLngExpression]]]

  type LatLngBoundsLiteral = js.Array[LatLngTuple]
  type LatLngBoundsExpression = LatLngBounds | LatLngBoundsLiteral

}
