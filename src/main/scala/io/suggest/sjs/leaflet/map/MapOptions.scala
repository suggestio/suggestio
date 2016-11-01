package io.suggest.sjs.leaflet.map

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.11.15 16:54
 * Description: L.map() options object.
 * @see [[http://leafletjs.com/reference.html#map-options]]
 */
@js.native
class MapOptions extends js.Object {

  var center                : LatLng        = js.native
  var zoom                  : Double        = js.native
  var layers                : js.Array[ILayer] = js.native
  var minZoom               : Double        = js.native
  var maxZoom               : Double        = js.native
  var maxBounds             : LatLngBounds  = js.native

  // Interaction Options
  var dragging              : Boolean       = js.native
  var touchZoom             : Boolean       = js.native
  var scrollWheelZoom       : Boolean       = js.native
  var doubleClickZoom       : Boolean       = js.native
  var boxZoom               : Boolean       = js.native
  var tap                   : Boolean       = js.native
  var tapTolerance          : Int           = js.native
  var trackResize           : Boolean       = js.native
  var worldCopyJump         : Boolean       = js.native
  var closePopupOnClick     : Boolean       = js.native
  var bounceAtZoomLimits    : Boolean       = js.native

  // Keyboard Navigation Options
  var keyboard              : Boolean       = js.native
  var keyboardPanOffset     : Int           = js.native
  var keyboardZoomOffset    : Int           = js.native

  // Panning Inertia Options
  var inertia               : Boolean       = js.native
  var inertiaDeceleration   : Double        = js.native
  var inertiaMaxSpeed       : Double        = js.native
  var inertiaThreshold      : Double        = js.native

  // Control options
  var zoomControl           : Boolean       = js.native
  var attributionControl    : Boolean       = js.native

  // Animation options
  var fadeAnimation         : Boolean       = js.native
  var zoomAnimation         : Boolean       = js.native
  var zoomAnimationThreshold: Double        = js.native
  var markerZoomAnimation   : Boolean       = js.native

}
