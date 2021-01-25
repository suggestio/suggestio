package io.suggest.sjs.leaflet.control.attribution

import io.suggest.sjs.leaflet.LEAFLET_IMPORT
import io.suggest.sjs.leaflet.control.{Control, ControlOptions}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.01.2021 18:42
  */
@js.native
@JSImport(LEAFLET_IMPORT, "Attribution")
class Attribution(override val options: AttributionOptions = js.native) extends Control {
  def setPrefix(prefix: String | Boolean): this.type = js.native
  def addAttribution(text: String): this.type = js.native
  def removeAttribution(text: String): this.type = js.native
}


trait AttributionOptions extends ControlOptions {
  val prefix: js.UndefOr[String | Boolean] = js.undefined
}
