package io.suggest.sjs.leaflet.div

import io.suggest.sjs.leaflet.LEAFLET_IMPORT
import io.suggest.sjs.leaflet.map.{Layer, Point}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.2021 18:58
  * Description: @see [[https://leafletjs.com/reference-1.6.0.html#divoverlay]]
  */
@JSImport(LEAFLET_IMPORT, "DivOverlay")
@js.native
class DivOverlay extends Layer


trait DivOverlayOptions extends js.Object {

  val offset: js.UndefOr[Point]

  val className: js.UndefOr[String] = js.undefined

  val pane: js.UndefOr[String] = js.undefined

}