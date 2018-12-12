package io.suggest.spa

import io.suggest.sjs.common.vm.attr.StringInputValueT
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.rm.SelfRemoveT
import org.scalajs.dom.raw.HTMLInputElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.12.16 17:33
  * Description: vm'ка инпута для вычитывания сериализованного состояния.
  */

object StateInp extends FindElT {
  override def DOM_ID = SpaConst.STATE_CONT_ID
  override type Dom_t = HTMLInputElement
  override type T     = StateInp
}


import io.suggest.spa.StateInp.Dom_t

case class StateInp(override val _underlying: Dom_t) extends StringInputValueT with SelfRemoveT {

  override type T = Dom_t

}
