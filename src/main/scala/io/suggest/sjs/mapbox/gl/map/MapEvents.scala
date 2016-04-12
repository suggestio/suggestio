package io.suggest.sjs.mapbox.gl.map

import io.suggest.common.event._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 12:09
  * Description: Constant event names for a map.
  */
object MapEvents
  extends Touch
    with Mouse
    with Drag
    with Rotate
    with Move
    with Click
    with Zoom
    with Load
    with WebGlContext
    with BoxZoom
