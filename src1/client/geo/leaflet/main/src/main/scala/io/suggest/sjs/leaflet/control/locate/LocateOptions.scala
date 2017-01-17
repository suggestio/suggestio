package io.suggest.sjs.leaflet.control.locate

import io.suggest.sjs.leaflet.control.ControlOptions

import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined
import scala.scalajs.js.{Dictionary, UndefOr}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 12:07
  * Description: Locate constructor options model.
  */

@ScalaJSDefined
trait LocateOptions extends ControlOptions {

  //val layer: new L.LayerGroup(),  // use your own layer for the location marker

  // controls whether a circle is drawn that shows the uncertainty about the location [true]
  val drawCircle: UndefOr[Boolean] = js.undefined

  // follow the user's location [false]
  val follow: UndefOr[Boolean] = js.undefined

  // automatically sets the map view to the user's location, enabled if `follow` is true [true]
  val setView: UndefOr[Boolean] = js.undefined

  // keep the current map zoom level when displaying the user's location. (if `false`, use maxZoom) [false]
  val keepCurrentZoomLevel: UndefOr[Boolean] = js.undefined

  // stop following when the map is dragged if `follow` is true (deprecated, see below) [false]
  val stopFollowingOnDrag: UndefOr[Boolean] = js.undefined

  // if true locate control remains active on click even if the user's location is in view. [false]
  val remainActive: UndefOr[Boolean] = js.undefined

  //val markerClass: L.circleMarker, // L.circleMarker or L.marker
  //  circleStyle: {},  // change the style of the circle around the user's location
  //  markerStyle: {},
  //  followCircleStyle: {},  // set difference for the style of the circle around the user's location while following
  //  followMarkerStyle: {},

  // class for icon, fa-location-arrow or fa-map-marker  ['fa fa-map-marker']
  val icon: UndefOr[String] = js.undefined

  // class for loading icon  ['fa fa-spinner fa-spin']
  val iconLoading: UndefOr[String] = js.undefined

  // padding around accuracy circle, value is passed to setBounds, [0, 0]
  val circlePadding: js.UndefOr[js.Array[Int]] = js.undefined

  // use metric or imperial units [true]
  val metric: js.UndefOr[Boolean] = js.undefined

  //onLocationError: function(err) {alert(err.message)},  // define an error callback function

  //onLocationOutsideMapBounds:  function(context) { // called when outside map boundaries
  //          alert(context.options.strings.outsideMapBoundsMsg);
  //},

  val showPopup: UndefOr[Boolean] = js.undefined // display a popup when the user click on the inner marker [true]

  val strings: UndefOr[Dictionary[String]] = js.undefined
  /*  {
        title: "Show me where I am",  // title of the locate control
        metersUnit: "meters", // string for metric units
        feetUnit: "feet", // string for imperial units
        popup: "You are within {distance} {unit} from this point",  // text to appear if user clicks on circle
        outsideMapBoundsMsg: "You seem located outside the boundaries of the map" // default message for onLocationOutsideMapBounds
    },
  */

  // define location options e.g enableHighAccuracy: true or maxZoom: 10
  val locateOptions: js.UndefOr[js.Object] = js.undefined

}
