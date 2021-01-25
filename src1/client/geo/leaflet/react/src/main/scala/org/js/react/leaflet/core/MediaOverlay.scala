package org.js.react.leaflet.core

import io.suggest.sjs.leaflet.LatLngBoundsExpression
import io.suggest.sjs.leaflet.overlay.{ImageOverlay, ImageOverlayOptions, SvgOverlay, VideoOverlay}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.01.2021 18:11
  */

trait MediaOverlayProps extends ImageOverlayOptions with InteractiveLayerProps {
  val bounds: LatLngBoundsExpression
}


@js.native
@JSImport(PACKAGE_NAME, "updateMediaOverlay")
object updateMediaOverlay extends js.Function {

  def apply[E <: ImageOverlay | SvgOverlay | VideoOverlay, P <: MediaOverlayProps]
           (overlay: E, props: P, prevProps: P)
           : Unit = js.native

}
