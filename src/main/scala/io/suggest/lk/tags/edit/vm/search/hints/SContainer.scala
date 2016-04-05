package io.suggest.lk.tags.edit.vm.search.hints

import io.suggest.common.tags.edit.TagsEditConstants.Search
import io.suggest.lk.tags.edit.m.signals.TagFoundClick
import io.suggest.sjs.common.fsm.{IInitLayoutFsm, SjsFsm}
import io.suggest.sjs.common.vm.content.{ClearT, SetInnerHtml}
import io.suggest.sjs.common.vm.evtg.OnMouseClickT
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.style.{ShowHideDisplayT, StyleLeft, StyleTop}
import org.scalajs.dom.Event

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.16 21:49
  * Description: Контейнер выпадающего списка найденных тегов.
  */
object SContainer extends FindDiv {
  override type T     = SContainer
  override def DOM_ID = Search.FOUND_TAGS_CONT_ID
}


import SContainer.Dom_t

trait AFoundTagsContT
  extends SetInnerHtml
    with ShowHideDisplayT
    with StyleLeft
    with StyleTop
    with ClearT
    with OnMouseClickT
    with IInitLayoutFsm
{

  override type T = Dom_t

  override def initLayout(fsm: SjsFsm): Unit = {
    // Нужно слушать клики по рядам в списке и
    onClick { e: Event =>
      for (srow <- SRow.ofEventTargetUp( e.target )) {
        fsm !! TagFoundClick(srow, e)
      }
    }
  }

}


case class SContainer(
  override val _underlying: Dom_t
)
  extends AFoundTagsContT
