package io.suggest.sjs.leaflet.control.scale

import io.suggest.sjs.leaflet.LEAFLET_IMPORT
import io.suggest.sjs.leaflet.control.{Control, ControlOptions}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.01.2021 15:17
  */
trait ScaleOptions extends ControlOptions {
  val maxWidth: js.UndefOr[Double] = js.undefined
  val metric: js.UndefOr[Boolean] = js.undefined
  val imperial: js.UndefOr[Boolean] = js.undefined
  val updateWhenIdle: js.UndefOr[Boolean] = js.undefined
}


@js.native
@JSImport(LEAFLET_IMPORT, "Control.Scale")
class Scale(
             override val options: ScaleOptions = js.native,
           )
  extends Control
