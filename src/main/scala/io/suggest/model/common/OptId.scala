package io.suggest.model.common

import io.suggest.primo.IId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.02.16 18:50
  * Description: Интерфейс опционального id для почти всех инстансов БД-моделей.
  * Тип id вынесен в параметр, это позволяет абстрагировать статическую утиль от всех моделей.
  */

trait OptId[Id_t] extends IId[Option[Id_t]] {
}


object OptId {

  /** Приведение коллекции инстансов к коллекции id'шников.
    *
    * @param els Исходная коллекция инстансов.
    * @tparam Id_t Тип id'шника.
    * @return Итератор id'шников.
    */
  def els2ids[Id_t](els: TraversableOnce[OptId[Id_t]]): Iterator[Id_t] = {
    els.toIterator
      .flatMap(_.id)
  }

  /** Приведение коллекции инстансов ко множеству id'шников. */
  def els2idsSet[Id_t](els: TraversableOnce[OptId[Id_t]]): Set[Id_t] = {
    els2ids(els)
      .toSet
  }

  /** Приведение списка элеменов в итератору, пригодному к дальнейшему конвертацию в карту. */
  def els2idMapIter[Id_t, T <: OptId[Id_t]](els: TraversableOnce[T]): Iterator[(Id_t, T)] = {
    if (els.isEmpty) {
      Iterator.empty
    } else {
      els
        .toIterator
        .flatMap { el =>
          for (id <- el.id) yield {
            id -> el
          }
        }
    }
  }

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
  def els2idMap[Id_t, T <: OptId[Id_t]](els: TraversableOnce[T]): Map[Id_t, T] = {
    if (els.isEmpty)
      Map.empty
    else
      els2idMapIter[Id_t, T](els).toMap
  }


  /**
    * Входящий набор опциональных id'шников в итератор из просто id'шников.
    *
    * @param optIds Входящий набор Option[Id].
    * @tparam Id_t Тип id.
    * @return Итератор элементов типа Id_t.
    */
  def optIds2ids[Id_t](optIds: TraversableOnce[Option[Id_t]]): Iterator[Id_t] = {
    optIds
      .toIterator
      .flatMap(_.iterator)
  }


  /** Отсортировать элементы моделей согласно порядку их id.
    *
    * @param ids Исходные id'шники в исходном порядке.
    * @param els Исходная цепочка элементов.
    * @tparam Id_t Тип используемого id'шника.
    * @tparam T Тип одного элемента модели.
    * @return Итоговая отсортированная коллекция.
    */
  def orderByIds[Id_t, T <: OptId[Id_t]](ids: TraversableOnce[Id_t], els: Seq[T]): Seq[T] = {
    val idsMap = ids.toIterator.zipWithIndex.toMap
    els.sortBy { e =>
      e.id
        .flatMap(idsMap.get)
        .getOrElse(0)
    }
  }

}
