package io.suggest.sjs.leaflet.control

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.ScalaJSDefined

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.10.16 18:56
  * Description: API for common control options.
  */
@ScalaJSDefined
trait ControlOptions extends js.Object {

  /** @see [[ControlPositions]]. */
  val position: UndefOr[String] = js.undefined

}
