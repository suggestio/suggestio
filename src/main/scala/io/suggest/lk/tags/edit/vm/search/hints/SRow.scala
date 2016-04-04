package io.suggest.lk.tags.edit.vm.search.hints

import io.suggest.common.tags.edit.TagsEditConstants.Search.Hints.HINT_ROW_CLASS
import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.child.OfMyCssClass
import io.suggest.sjs.common.vm.find.IApplyEl
import io.suggest.sjs.common.vm.of.{OfDiv, OfEventTargetNode}
import org.scalajs.dom.raw.HTMLDivElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.16 16:11
  * Description: VM'ка динамического ряда в списке результатов поиска тегов.
  */
object SRow extends IApplyEl with OfEventTargetNode with OfDiv with OfMyCssClass {

  override type Dom_t = HTMLDivElement
  override type T     = SRow

  override def VM_CSS_CLASS = HINT_ROW_CLASS

}


import SRow.Dom_t

trait SRowT extends IVm {
  override type T = Dom_t
}


case class SRow( override val _underlying: Dom_t )
  extends SRowT
