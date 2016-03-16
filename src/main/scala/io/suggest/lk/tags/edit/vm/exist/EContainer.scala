package io.suggest.lk.tags.edit.vm.exist

import io.suggest.common.tags.edit.TagsEditConstants._
import io.suggest.lk.tags.edit.m.signals.DeleteClick
import io.suggest.sjs.common.fsm.InitLayoutFsmClickT
import io.suggest.sjs.common.vm.content.SetInnerHtml
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.of.{OfEventTargetNode, ChildrenVms}
import org.scalajs.dom.raw.{HTMLElement, HTMLDivElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.09.15 13:14
 * Description: Контейнер для уже добавленных (существующих) тегов.
 */
object EContainer extends FindDiv with OfEventTargetNode {
  
  override type T = EContainer
  override def DOM_ID = EXIST_CONT_ID

  override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    el.id == DOM_ID
  }
}


trait EContainerT extends SetInnerHtml with InitLayoutFsmClickT with ChildrenVms {

  override type T = HTMLDivElement

  // Интеграция с ChildrenVms.
  override type ChildVm_t = ETagCont
  override protected def _childVmStatic = ETagCont

  /** Пройтись по всем тегав этом контейнере. */
  def tagsIterator = _childrenVms

  /** Статический компаньон модели для сборки сообщений. */
  override protected[this] def _clickMsgModel = DeleteClick

}


case class EContainer(
  override val _underlying: HTMLDivElement
)
  extends EContainerT
