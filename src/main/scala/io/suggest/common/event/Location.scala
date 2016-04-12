package io.suggest.common.event

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.16 12:26
  * Description: Constants for location event names.
  */
trait Location {

  /** Current user location found. */
  def LOCATION_FOUND = "locationfound"

  /** Unable to locate user position coordinates.. */
  def LOCATION_ERROR = "locationerror"

}


trait Following {

  /** Started to following user corrdinates on the map. */
  def START_FOLLOWING = "startfollowing"
  def STOP_FOLLOWING  = "stopfollowing"

}
