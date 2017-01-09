package io.suggest.maps.vm

import io.suggest.common.maps.MapFormConstants
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.FindDiv

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 19:20
  * Description: ViewModel for map container div.
  */
object MapContainer extends FindDiv {
  override type T     = MapContainer
  override def DOM_ID = MapFormConstants.CONTAINER_ID
}


import io.suggest.maps.vm.MapContainer.Dom_t


trait RadMapContainerT extends VmT {
  override type T = Dom_t
}


case class MapContainer(
  override val _underlying: Dom_t
)
  extends RadMapContainerT
