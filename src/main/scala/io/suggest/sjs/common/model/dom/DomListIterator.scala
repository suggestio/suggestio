package io.suggest.sjs.common.model.dom

import org.scalajs.dom.raw.DOMList
import scala.collection.AbstractIterator

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 18:46
 * Description: Поддержка iterator-обхода sjs-коллекций DOMList.
 */
object DomListIterator {

  /**
   * Завернуть DOMList в стандартный scala Iterator.
   * Считается, что DOMList не изменяется в длине по мере обхода этого списка.
   * @param d Экземпляр DOMList.
   * @tparam T Тип элементов.
   * @return Одноразовый итератор.
   */
  def apply[T](d: DOMList[T]): Iterator[T] = {
    new AbstractIterator[T] {
      /** На всякий случай кешируем длину.
        * Вдруг ведь length() окажется больше O(1) или потребует переключения контекста. */
      val l = d.length
      /** Индекс следующего элемента. */
      var i = 0

      override def hasNext: Boolean = {
        i < l
      }

      override def next(): T = {
        val res = d(i)
        i += 1
        res
      }
    }
  }

}
