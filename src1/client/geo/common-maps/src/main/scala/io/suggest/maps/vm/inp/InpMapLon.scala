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
object InpMapLon extends FindElT {
  override type T     = InpMapLon
  override def DOM_ID = MapFormConstants.INPUT_ID_LON
  override type Dom_t = HTMLInputElement
}


import InpMapLon.Dom_t


trait InpMapLonT extends DoubleInputValueT {
  override type T = Dom_t
}


case class InpMapLon(
  override val _underlying: Dom_t
)
  extends InpMapLonT
