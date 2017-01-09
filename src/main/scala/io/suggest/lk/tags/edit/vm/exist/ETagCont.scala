package io.suggest.lk.tags.edit.vm.exist

import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.vm.{Vm, VmT}
import io.suggest.sjs.common.vm.find.IApplyEl
import io.suggest.sjs.common.vm.of.OfEventTargetNode
import org.scalajs.dom.raw.{HTMLElement, HTMLDivElement}
import io.suggest.common.tags.edit.TagsEditConstants.ONE_EXISTING_CONT_CLASS

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.09.15 15:00
 * Description: Модель для списка динамических тегов.
 */

object ETagCont extends IApplyEl with OfEventTargetNode {

  override type T = ETagCont
  override type Dom_t = HTMLDivElement

  override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    Vm(el).containsClass(ONE_EXISTING_CONT_CLASS)
  }

}


import ETagCont.Dom_t


trait ETagContT extends VmT  {

  override type T = Dom_t

  /** Найти и вернуть input hidden. */
  def input: Option[ETagField] = {
    DomListIterator(_underlying.children)
      .flatMap(ETagField.ofElUnsafe)
      .toStream
      .headOption
  }

  /** Аккуратно скрыть элемент, а затем удалить. */
  def hideAndRemove(): Unit = {
    // TODO Прикрутить анимацию.
    remove()
  }

}


case class ETagCont(
  override val _underlying: Dom_t
)
  extends ETagContT
