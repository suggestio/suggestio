package io.suggest.primo.id

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.10.16 18:01
  * Description: Интерфейс поля с полем id произвольного типа.
  */
trait IId[T] {

  def id: T

}


/** Интерфейс type-class'а, который извлекает id из произвольного инстанса. */
trait IdGetter[T, Id_t] {
  def getId(t: T): Id_t
}

trait IdUtil[IId_t[_]] {

  /** Приведение коллекции инстансов к коллекции id'шников.
    *
    * @param els Исходная коллекция инстансов.
    * @tparam Id_t Тип id'шника.
    * @return Итератор id'шников.
    */
  def els2ids[Id_t](els: TraversableOnce[IId_t[Id_t]]): Iterator[Id_t]

  /** Приведение коллекции инстансов ко множеству id'шников. */
  def els2idsSet[Id_t](els: TraversableOnce[IId_t[Id_t]]): Set[Id_t] = {
    els2ids(els)
      .toSet
  }


  /** Приведение списка элеменов в итератору, пригодному к дальнейшему конвертацию в карту. */
  def els2idMapIter[Id_t, T <: IId_t[Id_t]](els: TraversableOnce[T]): Iterator[(Id_t, T)]

  /**
    * Приведение списка элементов к карте по id.
    * Типы, скорее всего, придётся описывать вручную при каждом вызове.
    *
    * @param els Исходный список элементов.
    * @tparam Id_t Тип используемого в коллекции id.
    * @tparam T Тип элемента.
    * @return Карта элементов, где ключ -- это id.
    *         Если id был пуст, то элемент будет отсутствовать в карте.
    */
  def els2idMap[Id_t, T <: IId_t[Id_t]](els: TraversableOnce[T]): Map[Id_t, T] = {
    if (els.isEmpty)
      Map.empty
    else
      els2idMapIter[Id_t, T](els)
        .toMap
  }

  /** Аналог els2idMap, но вокруг фьючерса. Появился просто из-за частой необходимости. */
  def elsFut2idMapFut[Id_t, T <: IId_t[Id_t]](elsFut: Future[TraversableOnce[T]])
                                             (implicit ec: ExecutionContext): Future[Map[Id_t, T]] = {
    for (els <- elsFut) yield {
      els2idMap(els)
    }
  }

}


object IId extends IdUtil[IId] {

  override def els2ids[Id_t](els: TraversableOnce[IId[Id_t]]): Iterator[Id_t] = {
    els.toIterator
      .map(_.id)
  }


  /** Приведение списка элеменов в итератору, пригодному к дальнейшему конвертацию в карту. */
  override def els2idMapIter[Id_t, T <: IId[Id_t]](els: TraversableOnce[T]): Iterator[(Id_t, T)] = {
    if (els.isEmpty) {
      Iterator.empty
    } else {
      els
        .toIterator
        .map { el =>
          el.id -> el
        }
    }
  }

}
