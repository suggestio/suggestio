package io.suggest.sjs.leaflet.event

import io.suggest.common.event.{Drag, Following, Location}

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
{

  def DRAG_PRE   = "predrag"

}
