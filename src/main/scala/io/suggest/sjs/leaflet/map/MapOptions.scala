package io.suggest.sjs.leaflet.map

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 16:54
  * Description: L.map() options object.
  */
@js.native
sealed trait MapOptions extends js.Object {

  var center    : LatLng            = js.native
  var zoom      : Double            = js.native
  var layers    : js.Array[ILayer]  = js.native
  var minZoom   : Double            = js.native
  var maxZoom   : Double            = js.native
  var maxBounds : LatLngBounds      = js.native

}
