package io.suggest.lk.tags.edit.vm.search.hints

import io.suggest.common.tags.edit.TagsEditConstants.Search.Hints._
import io.suggest.sjs.common.vm.attr.AttrVmT
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

  override type T     = SRow

  override def VM_CSS_CLASS = HINT_ROW_CLASS

}


import SRow.Dom_t

trait SRowT extends AttrVmT {

  override type T = Dom_t

  def tagFace = getAttribute( ATTR_TAG_FACE )

}


case class SRow( override val _underlying: Dom_t )
  extends SRowT
