package io.suggest.maps.vm.inp

import io.suggest.common.maps.MapFormConstants
import io.suggest.sjs.common.vm.attr.IntInputValueT
import io.suggest.sjs.common.vm.find.FindElT
import org.scalajs.dom.raw.HTMLInputElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 11:12
  * Description: VM'ка для значения зума карты.
  */
object InpMapZoom extends FindElT {
  override type T     = InpMapZoom
  override def DOM_ID = MapFormConstants.INPUT_ID_ZOOM
  override type Dom_t = HTMLInputElement
}


import InpMapZoom.Dom_t


trait InpMapZoomT extends IntInputValueT {
  override type T = Dom_t
}


case class InpMapZoom(
  override val _underlying: Dom_t
)
  extends InpMapZoomT
