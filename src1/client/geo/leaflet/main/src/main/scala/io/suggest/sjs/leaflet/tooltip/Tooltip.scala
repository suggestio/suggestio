package io.suggest.sjs.leaflet.tooltip

import io.suggest.sjs.leaflet.LEAFLET_IMPORT
import io.suggest.sjs.leaflet.div.{DivOverlay, DivOverlayOptions}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.01.2021 18:55
  * @see [[https://leafletjs.com/reference-1.6.0.html#tooltip]]
  */
@JSImport(LEAFLET_IMPORT, "Tooltip")
@js.native
class Tooltip extends DivOverlay


trait TooltipOptions extends DivOverlayOptions {
  val direction: js.UndefOr[String] = js.undefined
  val permanent,
      sticky,
      interactive
      : js.UndefOr[Boolean] = js.undefined
  val opacity: js.UndefOr[Double] = js.undefined
}