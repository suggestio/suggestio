package io.suggest.sjs.mapbox.gl

import io.suggest.sjs.mapbox.gl.map.SupportedOptions

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 11:11
  * Description: Top-level API for Mapbox GL.
  */
@js.native
@JSName("mapboxgl")
object mapboxgl extends js.Object {

  def supported(options: SupportedOptions = js.native): Boolean = js.native

  var accessToken: String = js.native

}
