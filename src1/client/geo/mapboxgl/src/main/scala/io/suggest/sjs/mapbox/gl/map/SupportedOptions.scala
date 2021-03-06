package io.suggest.sjs.mapbox.gl.map

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 15:22
  * Description: API for options of mapboxgl.supported().
  */

trait SupportedOptions extends js.Object {

  val failIfMajorPerformanceCaveat: UndefOr[Boolean] = js.undefined

}
