package io.suggest.maps.vm.inp

import io.suggest.common.maps.MapFormConstants
import io.suggest.sjs.common.vm.attr.DoubleInputValueT
import io.suggest.sjs.common.vm.find.FindElT
import org.scalajs.dom.raw.HTMLInputElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.11.15 10:52
  * Description: Vm'ка инпута, используемая для хранения значения latitude.
  */
object InpMapLat extends FindElT {
  override type T     = InpMapLat
  override def DOM_ID = MapFormConstants.INPUT_ID_LAT
  override type Dom_t = HTMLInputElement
}


import InpMapLat.Dom_t


trait InpMapLatT extends DoubleInputValueT {
  override type T = Dom_t
}


case class InpMapLat(
  override val _underlying: Dom_t
)
  extends InpMapLatT
