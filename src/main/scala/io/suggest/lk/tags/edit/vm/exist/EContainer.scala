package io.suggest.lk.tags.edit.vm.exist

import io.suggest.lk.tags.edit.m.signals.DeleteClick
import io.suggest.lk.tags.edit.vm.util.InitOnClickToTagsEditFsmT
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.vm.content.SetInnerHtml
import io.suggest.sjs.common.vm.find.FindDiv
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.common.tags.edit.TagsEditConstants._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.09.15 13:14
 * Description: Контейнер для уже добавленных (существующих) тегов.
 */
object EContainer extends FindDiv {
  
  override type T = EContainer
  
  override def DOM_ID = EXIST_CONT_ID
  
}


trait EContainerT extends SetInnerHtml with InitOnClickToTagsEditFsmT {

  override type T = HTMLDivElement

  /** Пройтись по всем тегав этом контейнере. */
  def tagsIterator: Iterator[ETagCont] = {
    // TODO Задействовать vm.of.ChildrenVms
    DomListIterator( _underlying.children )
      .flatMap { ETagCont.maybeApply }
  }

  /** Статический компаньон модели для сборки сообщений. */
  override protected[this] def _clickMsgModel = DeleteClick

}


case class EContainer(
  override val _underlying: HTMLDivElement
)
  extends EContainerT
