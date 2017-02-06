package io.suggest.lk.adn.map.vm

import io.suggest.adn.AdnMapFormConstants
import io.suggest.sjs.common.controller.DomQuick
import io.suggest.sjs.common.fsm.{IInitLayoutFsm, SjsFsm}
import io.suggest.sjs.common.vm.attr.IntInputValueT
import io.suggest.sjs.common.vm.find.FindElT
import org.scalajs.dom.raw.HTMLInputElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.02.17 11:55
  * Description: vm'ка для поля adn-map-формы с tzOffset'ом.
  */
object InpTzOffset extends FindElT {
  override def DOM_ID = AdnMapFormConstants.Fields.TZ_OFFSET_ID
  override type Dom_t = HTMLInputElement
  override type T = InpTzOffset
}

import InpTzOffset.Dom_t

case class InpTzOffset(override val _underlying: Dom_t) extends IntInputValueT with IInitLayoutFsm {

  override type T = Dom_t

  override def initLayout(fsm: SjsFsm): Unit = {
    value = DomQuick.tzOffsetMinutes
  }

}
