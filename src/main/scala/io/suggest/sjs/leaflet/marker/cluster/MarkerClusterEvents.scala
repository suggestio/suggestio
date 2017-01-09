package io.suggest.sjs.leaflet.marker.cluster

import io.suggest.sjs.leaflet.event.Events

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.16 21:12
  * Description: Event types constants.
  */
object MarkerClusterEvents {

  final val CLICK             = Events.CLICK

  final val CLUSTER_CLICK     = "cluster" + CLICK

  final val ANIMATION_END     = "animationend"

  final val SPIDERFIED        = "spiderfied"

  final val UNSPIDERFIED      = "un" + SPIDERFIED

}
