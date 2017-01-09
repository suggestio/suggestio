package io.suggest.sjs.mapbox

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 13:49
  * Description: Common types.
  */
package object gl {

  type Zoom_t = Double

  type Bearing_t = Double

  type Pitch_t   = Double

  type Color_t   = String


  type FilterEl_t = js.Any

  type Filter_t  = js.Array[FilterEl_t]

}
