package io.suggest.sc.sjs.vm.search.tabs.htag

import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.{FindElDynIdT, IApplyEl}
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Search.TagsTab._
import io.suggest.sjs.common.vm.attr.UnderlyingIdT
import io.suggest.sjs.common.vm.util.{DomIdPrefixed, DynDomIdRawString}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.09.15 17:38
 * Description: vm'ка одного ряда в списке тегов.
 */
object StListRow
  extends IApplyEl
  with FindElDynIdT
  with DynDomIdRawString
  with DomIdPrefixed
{

  override type T             = StListRow
  override type Dom_t         = HTMLDivElement

  override def DOM_ID_PREFIX  = ROW_ID_PREFIX

}

import StListRow.Dom_t


/** Логика vm'ки одного ряда-тега в списке тегов вынесена в трейт. */
trait StListRowT extends VmT with UnderlyingIdT {

  override type T = Dom_t

  /** id узла-тега. */
  def nodeId = getAttribute(ATTR_NODE_ID)

  def tagFace = _underlying.innerHTML.trim

  /** Выставить css-класс selected. */
  def select(): Unit = {
    addClasses( SELECTED_CSS_CLASS )
  }

  /** Убрать css-класс selected. */
  def unSelect(): Unit = {
    removeClass( SELECTED_CSS_CLASS )
  }

}


/** Реализация vm'ки одного ряда-тега в списке тегов. */
case class StListRow(
  override val _underlying: Dom_t
)
  extends StListRowT
