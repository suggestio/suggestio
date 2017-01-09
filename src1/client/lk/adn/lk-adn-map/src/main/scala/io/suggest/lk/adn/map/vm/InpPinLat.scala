package io.suggest.lk.adn.map.vm

import io.suggest.adn.AdnMapFormConstants
import io.suggest.sjs.common.vm.attr.DoubleInputValueT
import io.suggest.sjs.common.vm.find.FindElT
import org.scalajs.dom.raw.HTMLInputElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.16 17:05
  * Description: vm'ка для инпута формы с долготой точки размещения.
  */
object InpPinLat extends FindElT {
  override type Dom_t     = HTMLInputElement
  override type T         = InpPinLat
  override def DOM_ID     = AdnMapFormConstants.Point.INPUT_ID_LAT
}


import InpPinLat.Dom_t

case class InpPinLat(override val _underlying: Dom_t) extends DoubleInputValueT {
  override type T = Dom_t
}
