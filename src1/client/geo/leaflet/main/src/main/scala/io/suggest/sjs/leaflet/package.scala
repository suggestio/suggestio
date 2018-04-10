package io.suggest.sjs

import io.suggest.sjs.leaflet.map.LatLng
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js
import scala.scalajs.js.`|`

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 16:45
  */
package object leaflet {

  //final val LEAFLET_IMPORT = "string-replace-loader?search=window.L&replace=window.LLLLLLLL!./node_modules/leaflet/dist/leaflet.js"
  final val LEAFLET_IMPORT = "leaflet"

  type MapTarget = String | HTMLElement

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
  type PolygonLatLngs_t = js.Array[LatLng] | js.Array[js.Array[LatLng]] | js.Array[js.Array[js.Array[LatLng]]]

  type PolygonCoords_t = js.Array[Double] | js.Array[js.Array[Double]] | js.Array[js.Array[js.Array[Double]]] | js.Array[js.Array[js.Array[js.Array[Double]]]]

}
