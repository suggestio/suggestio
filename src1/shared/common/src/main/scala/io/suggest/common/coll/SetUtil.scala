package io.suggest.common.coll

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 21:01
  * Description: Полезная утиль для множеств.
  */
object SetUtil {

  /** diode сильно ориентирован на FastEq сравнение.
    * Тут мы пытаемся залить в исходное множество новые элементы,
    * но если ничего не изменилось, то вернуть исходный указатель на исходное множество.
    */
  def addToSetOrKeepRef1[T](set0: Set[T], newEls: TraversableOnce[T]): Set[T] = {
    val set1 = set0 ++ newEls
    if (set1.size > set0.size) set1 else set0
  }
  def addToSetOrKeepRef[T](set0: Set[T], newEls: T*): Set[T] = {
    addToSetOrKeepRef1(set0, newEls)
  }


  def delFromSetOrKeepRef[T](set0: Set[T], els: T*): Set[T] = {
    delFromSetOrKeepRef(set0, els)
  }
  def delFromSetOrKeepRef[T](set0: Set[T], els: TraversableOnce[T]): Set[T] = {
    val set1 = set0 -- els
    if (set1.size < set0.size) set1 else set0
  }

}
