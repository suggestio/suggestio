package io.suggest.sjs.leaflet.event

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.11.15 15:26
  * Description: Some constants for event names.
  */
object Events {

  def DRAG_START = "dragstart"

  def DRAG_PRE   = "predrag"

  def DRAG       = "drag"

  /** User finished to drag the map. */
  def DRAG_END   = "dragend"

  /** Current user location found. */
  def LOCATION_FOUND = "locationfound"

  /** Unable to locate user position coordinates.. */
  def LOCATION_ERROR = "locationerror"

  /** Started to following user corrdinates on the map. */
  def START_FOLLOWING = "startfollowing"
  def STOP_FOLLOWING  = "stopfollowing"

}
