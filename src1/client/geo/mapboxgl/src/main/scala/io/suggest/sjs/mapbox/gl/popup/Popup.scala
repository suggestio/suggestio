package io.suggest.sjs.mapbox.gl.popup

import io.suggest.sjs.mapbox.gl.control.Control
import io.suggest.sjs.mapbox.gl.event.Evented
import io.suggest.sjs.mapbox.gl.ll.LngLat

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 15:44
  * Description: API for popup control.
  */
@js.native
@JSGlobal("mapboxgl.Popup")
class Popup extends Control with Evented {

  def getLngLat(): LngLat = js.native

  def setHTML(html: String): this.type = js.native

  def setLngLat(lngLat: LngLat): this.type = js.native

  def setText(text: String): this.type = js.native

}
