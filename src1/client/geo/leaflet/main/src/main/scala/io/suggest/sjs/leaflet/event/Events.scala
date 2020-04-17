package io.suggest.sjs.leaflet.event

import io.suggest.event._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.11.15 15:26
  * Description: Some constants for event names.
  */
object Events
  extends Drag
  with Location
  with Following
  with Zoom
  with Click
{

  def DRAG_PRE   = "predrag"

}
