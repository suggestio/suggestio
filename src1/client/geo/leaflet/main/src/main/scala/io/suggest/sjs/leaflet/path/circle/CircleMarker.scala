package io.suggest.sjs.leaflet.path.circle

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.15 23:02
 * Description: API for L.CircleMarker.
 * Here radius in pixels, not meters.
 */
@JSImport("leaflet", "CircleMarker")
@js.native
class CircleMarker extends Circle
