package io.suggest.lk.tags.edit.vm.add

import io.suggest.common.tags.edit.TagsEditConstants.Search
import io.suggest.sjs.common.vm.content.{ClearT, SetInnerHtml}
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.style.{StyleTop, StyleLeft, ShowHideDisplayT}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.16 21:49
  * Description: Контейнер выпадающего списка найденных тегов.
  */
object AFoundTagsCont extends FindDiv {
  override type T     = AFoundTagsCont
  override def DOM_ID = Search.FOUND_TAGS_CONT_ID
}


import io.suggest.lk.tags.edit.vm.add.AFoundTagsCont.Dom_t

trait AFoundTagsContT extends SetInnerHtml with ShowHideDisplayT with StyleLeft with StyleTop with ClearT {

  override type T = Dom_t

}


case class AFoundTagsCont(
  override val _underlying: Dom_t
)
  extends AFoundTagsContT
