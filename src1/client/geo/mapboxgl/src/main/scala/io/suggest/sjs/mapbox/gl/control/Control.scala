package io.suggest.sjs.mapbox.gl.control

import io.suggest.sjs.mapbox.gl.map.GlMap

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 11:59
  * Description: API for abstract GL Map control.
  */
@js.native
//@JSGlobal("mapboxgl.Control")
trait Control extends js.Object {

  def addTo(map: GlMap): this.type = js.native

  def remove(): this.type = js.native

}
