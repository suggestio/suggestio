package io.suggest.sjs.dom2

import org.scalajs.dom.raw.DOMList

import scala.collection.{AbstractIterator, mutable}

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

case class DomListIterator[T](
  override val domList: DOMList[T]
)
  extends AbstractIterator[T]
    with DomListCollUtil[T]
{

  /** На всякий случай кешируем длину обрабатываемого списка.
    * Вдруг ведь length() окажется больше O(1) или потребует переключения контекста. */
  override val size = super.size

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


/** Реализация многоразовой zero-copy iterable-коллекции на основе [[DomListIterator]]. */
case class DomListSeq[T](
  override val domList: DOMList[T]
)
  extends mutable.AbstractSeq[T]
  with DomListCollUtil[T]
{
  override def iterator = DomListIterator(domList)
  override def length   = size
  override def apply(idx: Int): T = {
    domList(idx)
  }
  override def update(idx: Int, elem: T): Unit = {
    domList.update(idx, elem)
  }
}


/** Утиль для scala-коллекций вокруг DOMList. */
trait DomListCollUtil[T] extends IterableOnce[T] {

  /** Инстанс исходного DOMList'а. */
  val domList: DOMList[T]

  /** Оптимизация подсчета длины коллекции на базе DOMList.
    * Возможно даже до O(1), но это зависит реализации списка на стороне браузера. */
  override def knownSize: Int = domList.length

}

