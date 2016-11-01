package io.suggest.sjs.leaflet.control.locate

import io.suggest.sjs.common.model.FromDict
import io.suggest.sjs.leaflet.control.ControlOptions

import scala.scalajs.js
import scala.scalajs.js.Dictionary

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 12:07
  * Description: Locate constructor options model.
  */
object LocateOptions extends FromDict {
  override type T = LocateOptions
}

@js.native
class LocateOptions extends ControlOptions {

  //var layer: new L.LayerGroup(),  // use your own layer for the location marker

  // controls whether a circle is drawn that shows the uncertainty about the location [true]
  var drawCircle: Boolean = js.native

  // follow the user's location [false]
  var follow: Boolean = js.native

  // automatically sets the map view to the user's location, enabled if `follow` is true [true]
  var setView: Boolean = js.native

  // keep the current map zoom level when displaying the user's location. (if `false`, use maxZoom) [false]
  var keepCurrentZoomLevel: Boolean = js.native

  // stop following when the map is dragged if `follow` is true (deprecated, see below) [false]
  var stopFollowingOnDrag: Boolean = js.native

  // if true locate control remains active on click even if the user's location is in view. [false]
  var remainActive: Boolean = js.native

  //var markerClass: L.circleMarker, // L.circleMarker or L.marker
  //  circleStyle: {},  // change the style of the circle around the user's location
  //  markerStyle: {},
  //  followCircleStyle: {},  // set difference for the style of the circle around the user's location while following
  //  followMarkerStyle: {},

  // class for icon, fa-location-arrow or fa-map-marker  ['fa fa-map-marker']
  var icon: String = js.native

  // class for loading icon  ['fa fa-spinner fa-spin']
  var iconLoading: String = js.native

  // padding around accuracy circle, value is passed to setBounds, [0, 0]
  var circlePadding: js.Array[Int] = js.native

  // use metric or imperial units [true]
  var metric: Boolean = js.native

  //onLocationError: function(err) {alert(err.message)},  // define an error callback function

  //onLocationOutsideMapBounds:  function(context) { // called when outside map boundaries
  //          alert(context.options.strings.outsideMapBoundsMsg);
  //},

  var showPopup: Boolean = js.native // display a popup when the user click on the inner marker [true]

  var strings: Dictionary[String] = js.native
  /*  {
        title: "Show me where I am",  // title of the locate control
        metersUnit: "meters", // string for metric units
        feetUnit: "feet", // string for imperial units
        popup: "You are within {distance} {unit} from this point",  // text to appear if user clicks on circle
        outsideMapBoundsMsg: "You seem located outside the boundaries of the map" // default message for onLocationOutsideMapBounds
    },
  */

  // define location options e.g enableHighAccuracy: true or maxZoom: 10
  var locateOptions: js.Object = js.native

}
