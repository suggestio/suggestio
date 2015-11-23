package io.suggest.sjs.leaflet.map

import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.11.15 16:46
 * Description: API for map panes. (object literal returned by map.getPanes() )
 * @see [[http://leafletjs.com/reference.html#map-panes]]
 */
@js.native
trait MapPanes extends js.Object {

  var mapPane     : HTMLElement   = js.native

  var tilePane    : HTMLElement   = js.native

  var objectsPane : HTMLElement   = js.native

  var shadowPane  : HTMLElement   = js.native

  var overlayPane : HTMLElement   = js.native

  var markerPane  : HTMLElement   = js.native

  var popupPane   : HTMLElement   = js.native

}
