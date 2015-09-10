package io.suggest.lk.tags.edit.vm.exist

import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.IApplyEl
import org.scalajs.dom.Node
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.common.tags.edit.TagsEditConstants.ONE_EXISTING_CONT_CLASS

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.09.15 15:00
 * Description: Модель для списка динамических тегов.
 */

object ETagCont extends IApplyEl {

  override type T = ETagCont
  override type Dom_t = HTMLDivElement

  /** Из ноды сделать дело. */
  def maybeApply(node: Node): Option[ETagCont] = {
    // TODO Проверять localName/nodeName?
    val div = node.asInstanceOf[HTMLDivElement]
    val etc = ETagCont( div )
    if (etc.hasMarkerClass)
      Some(etc)
    else
      None
  }

}

trait ETagContT extends VmT  {

  override type T = HTMLDivElement

  /** Есть ли у backend-тега класс-флаг, который должен быть у любого div'а editable тега? */
  def hasMarkerClass: Boolean = {
    containsClass(ONE_EXISTING_CONT_CLASS)
  }

  /** Найти и вернуть input hidden. */
  def input: Option[ETagField] = {
    DomListIterator( _underlying.getElementsByTagName(ETagField.TAG_NAME) )
      .collectFirst { case e => e }
      .map { node =>
        ETagField( node.asInstanceOf[ ETagField.Dom_t ] )
      }
  }

  /** Аккуратно скрыть элемент, а затем удалить. */
  def hideAndRemove(): Unit = {
    // TODO Прикрутить анимацию.
    remove()
  }

}


case class ETagCont(
  override val _underlying: HTMLDivElement
)
  extends ETagContT
