package io.suggest.model.common

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.02.16 18:50
  * Description: Интерфейс опционального id для почти всех инстансов БД-моделей.
  * Тип id вынесен в параметр, это позволяет абстрагировать статическую утиль от всех моделей.
  */

trait OptId[Id_t] {
  /** id текущего инстанса модели. */
  def id: Option[Id_t]
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

  def els2idsSet[Id_t](els: TraversableOnce[OptId[Id_t]]): Set[Id_t] = {
    els2ids(els)
      .toSet
  }

}
