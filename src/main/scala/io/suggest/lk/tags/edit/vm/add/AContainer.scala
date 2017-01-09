package io.suggest.lk.tags.edit.vm.add

import io.suggest.common.tags.edit.TagsEditConstants.ADD_FORM_ID
import io.suggest.lk.tags.edit.vm.search.hints.SContainer
import io.suggest.sjs.common.fsm.{IInitLayoutFsm, SjsFsm}
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.content.ReplaceWith
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.of.{OfDiv, OfEventTargetNode, OfHtml}
import io.suggest.sjs.common.vm.util.OfHtmlElDomIdRelated

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.09.15 12:26
 * Description: Контейнер элементов формы добавления тега.
 */
object AContainer extends FindDiv with OfHtml with OfDiv with OfEventTargetNode with OfHtmlElDomIdRelated {

  override type T     = AContainer
  override def DOM_ID = ADD_FORM_ID

}

import AContainer.Dom_t


trait AContainerT extends IVm with IInitLayoutFsm with ReplaceWith {

  override type T = Dom_t

  /** Поиск input'а ввода имени тега. */
  def nameInput = ANameInput.find()

  def foundTagsCont = SContainer.find()

  override def initLayout(fsm: SjsFsm): Unit = {
    val f = IInitLayoutFsm.f(fsm)
    nameInput.foreach(f)
    foundTagsCont.foreach(f)
  }

}


case class AContainer(
  override val _underlying: Dom_t
)
  extends AContainerT
