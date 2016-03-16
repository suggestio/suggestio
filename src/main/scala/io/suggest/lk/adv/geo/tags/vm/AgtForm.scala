package io.suggest.lk.adv.geo.tags.vm

import io.suggest.adv.geo.tag.AgtFormConsts
import io.suggest.sjs.common.fsm.IInitLayoutFsm
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.input.FormDataVmT
import org.scalajs.dom.raw.HTMLFormElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.16 17:42
  * Description: VM'ка формы размещения карточки в геотегах.
  */
object AgtForm extends FindElT {
  override type Dom_t = HTMLFormElement
  override type T     = AgtForm
  override def DOM_ID = AgtFormConsts.FORM_ID
}


import AgtForm.Dom_t

trait AgtFormT extends FormDataVmT {
  override type T = Dom_t
}


case class AgtForm(override val _underlying: Dom_t)
  extends AgtFormT
