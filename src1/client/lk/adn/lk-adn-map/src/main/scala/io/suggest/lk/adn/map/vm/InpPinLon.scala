package io.suggest.lk.adn.map.vm

import io.suggest.adn.AdnMapFormConstants
import io.suggest.sjs.common.vm.attr.DoubleInputValueT
import io.suggest.sjs.common.vm.find.FindElT
import org.scalajs.dom.raw.HTMLInputElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.16 17:00
  * Description: vm'ка для form-input широты точки размещения узла.
  */
object InpPinLon extends FindElT {
  override type Dom_t     = HTMLInputElement
  override type T         = InpPinLon
  override def DOM_ID     = AdnMapFormConstants.Point.INPUT_ID_LON
}


import InpPinLon.Dom_t

case class InpPinLon(override val _underlying: Dom_t) extends DoubleInputValueT {
  override type T = Dom_t
}
