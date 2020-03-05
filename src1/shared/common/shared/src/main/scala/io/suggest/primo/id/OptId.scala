package io.suggest.primo.id

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.02.16 18:50
  * Description: Интерфейс опционального id для почти всех инстансов БД-моделей.
  * Тип id вынесен в параметр, это позволяет абстрагировать статическую утиль от всех моделей.
  */

trait OptId[Id_t] extends IId[Option[Id_t]]


object OptId {

  implicit def OptId2idOpt[Id_t](x: OptId[Id_t]): Option[Id_t] = x.id

}
