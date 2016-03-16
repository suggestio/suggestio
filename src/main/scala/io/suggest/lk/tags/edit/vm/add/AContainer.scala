package io.suggest.lk.tags.edit.vm.add

import io.suggest.common.tags.edit.TagsEditConstants.ADD_FORM_ID
import io.suggest.sjs.common.fsm.{IInitLayoutFsm, SjsFsm}
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.content.{ApplyFromOuterHtml, ReplaceWith}
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.of.{OfEventTargetNode, OfHtml}
import org.scalajs.dom.Node
import org.scalajs.dom.raw.{HTMLElement, HTMLDivElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 12:26
 * Description: Контейнер элементов формы добавления тега.
 */
object AContainer extends FindDiv with OfHtml with OfEventTargetNode {

  override type T     = AContainer
  override def DOM_ID = ADD_FORM_ID

  override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    el.id == DOM_ID
  }

}


trait AContainerT extends IVm with IInitLayoutFsm with ReplaceWith {

  override type T = HTMLDivElement

  /** Поиск input'а ввода имени тега. */
  def nameInput = ANameInput.find()

  override def initLayout(fsm: SjsFsm): Unit = {
    val f = IInitLayoutFsm.f(fsm)
    nameInput.foreach(f)
  }

}


case class AContainer(
  override val _underlying: HTMLDivElement
)
  extends AContainerT
