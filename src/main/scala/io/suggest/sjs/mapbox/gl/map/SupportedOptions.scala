package io.suggest.sjs.mapbox.gl.map

import io.suggest.sjs.common.model.FromDict

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 15:22
  * Description: API for options of mapboxgl.supported().
  */

object SupportedOptions extends FromDict {
  override type T = SupportedOptions
}

@js.native
trait SupportedOptions extends js.Object {

  var failIfMajorPerformanceCaveat: Boolean = js.native

}
