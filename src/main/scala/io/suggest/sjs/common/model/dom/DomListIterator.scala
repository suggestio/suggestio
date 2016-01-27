package io.suggest.sjs.common.model.dom

import org.scalajs.dom.raw.DOMList
import scala.collection.AbstractIterator

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 18:46
 * Description: Поддержка iterator-обхода sjs-коллекций DOMList.
 * Сначала тут был статический apply() с анонимным классом внутри,
 * теперь просто case-class для упрощения многоэтажности.
 *
 * Завернуть DOMList в стандартный scala Iterator.
 * Считается, что DOMList не изменяется в длине по мере обхода этого списка.
 *
 * @param domList Экземпляр DOMList.
 * @tparam T Тип элементов.
 */
case class DomListIterator[T](domList: DOMList[T]) extends AbstractIterator[T] {

  /** На всякий случай кешируем длину обрабатываемого списка.
    * Вдруг ведь length() окажется больше O(1) или потребует переключения контекста. */
  override val size = domList.length

  /** Индекс следующего элемента. */
  private var i = 0

  override def hasNext: Boolean = {
    i < size
  }

  override def next(): T = {
    val res = domList(i)
    i += 1
    res
  }

}
