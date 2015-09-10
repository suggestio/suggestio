package io.suggest.lk.tags.edit.vm.exist

import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.{Vm, IVm}
import io.suggest.sjs.common.vm.find.IApplyEl
import org.scalajs.dom.{Node, EventTarget}
import org.scalajs.dom.raw.HTMLElement
import io.suggest.common.tags.edit.TagsEditConstants.DELETE_EXISTING_CLASS

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.09.15 16:08
 * Description: VM'ка для элемента удаления тега.
 */
object EDelete extends IApplyEl {

  override type T       = EDelete

  override type Dom_t   = HTMLElement

  def maybeApply(node: EventTarget): Option[EDelete] = {
    val tgNode = node.asInstanceOf[ Node ]
    for (vm <- VUtil.hasCssClass( Vm(tgNode), DELETE_EXISTING_CLASS)) yield {
      apply( vm._underlying.asInstanceOf[Dom_t] )
    }
  }

}


import EDelete.Dom_t


trait EDeleteT extends IVm {

  override type T = Dom_t

  def tagContainer: ETagCont = {
    val parentDiv = _underlying.parentElement.asInstanceOf[ ETagCont.Dom_t ]
    ETagCont( parentDiv )
  }

}


case class EDelete(
  override val _underlying: Dom_t
)
  extends EDeleteT
