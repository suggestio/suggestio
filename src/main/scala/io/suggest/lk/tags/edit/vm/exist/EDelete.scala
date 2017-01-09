package io.suggest.lk.tags.edit.vm.exist

import io.suggest.common.tags.edit.TagsEditConstants.DELETE_EXISTING_CLASS
import io.suggest.sjs.common.vm.find.IApplyEl
import io.suggest.sjs.common.vm.of.OfEventTargetNode
import io.suggest.sjs.common.vm.{IVm, Vm}
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.09.15 16:08
 * Description: VM'ка для элемента удаления тега.
 */
object EDelete extends IApplyEl with OfEventTargetNode {

  override type T       = EDelete

  override type Dom_t   = HTMLElement

  override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    Vm(el).containsClass(DELETE_EXISTING_CLASS)
  }

}


import EDelete.Dom_t


trait EDeleteT extends IVm {

  override type T = Dom_t

  def tagContainer = ETagCont.ofHtmlEl( _underlying.parentElement )

}


case class EDelete(
  override val _underlying: Dom_t
)
  extends EDeleteT
