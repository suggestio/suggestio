package io.suggest.sjs.mapbox.gl.event

import io.suggest.common.event._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 12:09
  * Description: Constant event names for a map.
  */
object MapEventsTypes
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
{

  def STYLE_PREFIX = "style"

  def PREFIX_DELIM = "."

  /** style.load событие готовности карты. */
  def STYLE_LOADED = STYLE_PREFIX + PREFIX_DELIM + LOAD

}
