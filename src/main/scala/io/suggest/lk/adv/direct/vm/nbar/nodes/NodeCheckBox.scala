package io.suggest.lk.adv.direct.vm.nbar.nodes

import io.suggest.adv.direct.AdvDirectFormConstants
import io.suggest.sjs.common.vm.find.FindElDynIdT
import io.suggest.sjs.common.vm.input.{CheckBoxVmStaticT, CheckBoxVmT}
import io.suggest.sjs.common.vm.util.{DomIdPrefixed, DynDomIdRawString, OfHtmlElDomIdRelated}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 22:41
 * Description: Чекбокс одного узла.
 */
object NodeCheckBox
  extends FindElDynIdT
    with CheckBoxVmStaticT
    with DynDomIdRawString
    with DomIdPrefixed
    with OfHtmlElDomIdRelated
{

  override type DomIdArg_t    = String
  override type T             = NodeCheckBox
  override def DOM_ID_PREFIX  = AdvDirectFormConstants.NODE_CHECK_BOX_ID_PREFIX

}


import NodeCheckBox.Dom_t


trait NodeCheckBoxT extends CheckBoxVmT {

  override type T = Dom_t

  /** Найти текущий ряд N2-узла среди родительских элементов. */
  def nodeRow = NodeRow.ofNodeUp(_underlying.parentNode)

}


case class NodeCheckBox(override val _underlying: Dom_t)
  extends NodeCheckBoxT
