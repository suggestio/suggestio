package io.suggest.lk.tags.edit.vm.exist

import io.suggest.common.tags.edit.TagsEditConstants
import io.suggest.sjs.common.vm.attr.StringInputValueT
import io.suggest.sjs.common.vm.Vm
import io.suggest.sjs.common.vm.find.IApplyEl
import io.suggest.sjs.common.vm.of.{OfEventTargetNode, OfInput}
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.09.15 15:30
 * Description: vm одного тега для элемента input type=hidden.
 */
object ETagField extends IApplyEl with OfEventTargetNode with OfInput {

  override type T = ETagField
  override type Dom_t = HTMLInputElement

  override def _isWantedHtmlEl(el: HTMLElement): Boolean = {
    super._isWantedHtmlEl(el) && {
      Vm(el)
        .getAttribute("name")
        .exists { n =>
          n.startsWith(TagsEditConstants.EXIST_TAGS_FN) &&
          n.endsWith("." + TagsEditConstants.EXIST_TAG_NAME_FN)
        }
    }
  }

}


trait ETagFieldT extends StringInputValueT {

  override type T = HTMLInputElement

}


case class ETagField(
  override val _underlying: HTMLInputElement
)
  extends ETagFieldT
