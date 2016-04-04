package io.suggest.lk.tags.edit.vm.search.hints

import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.common.tags.edit.TagsEditConstants.Search.FOUND_TAGS_CONT_ID
import io.suggest.sjs.common.vm.child.ChildsByClassName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.16 16:09
  * Description: Контейнер найденных похожих тегов.
  */
object SContainer extends FindDiv {
  override def DOM_ID = FOUND_TAGS_CONT_ID
  override type T     = SContainer
}


import SContainer.Dom_t

trait SContainerT extends ChildsByClassName {

  override type T = Dom_t

  /** Вернуть ряды найденных тегов. */
  def rowsIter = _findChildsByClass(SRow)

}


case class SContainer( override val _underlying: Dom_t )
  extends SContainerT
