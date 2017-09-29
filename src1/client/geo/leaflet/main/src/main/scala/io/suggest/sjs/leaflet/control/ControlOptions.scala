package io.suggest.sjs.leaflet.control

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.10.16 18:56
  * Description: API for common control options.
  */
trait ControlOptions extends js.Object {

  /** @see [[ControlPositions]]. */
  val position: UndefOr[String] = js.undefined

}
