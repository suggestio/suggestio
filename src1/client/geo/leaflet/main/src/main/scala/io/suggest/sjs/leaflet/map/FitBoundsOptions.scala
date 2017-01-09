package io.suggest.sjs.leaflet.map

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 18:57
  * Description: Fit bounds options model.
  */
@js.native
sealed trait FitBoundsOptions extends js.Object {

  var paddingTopLeft:	Point = js.native

  var paddingBottomRight: Point = js.native

  var padding: Point = js.native

  var maxZoom: Double = js.native

}
