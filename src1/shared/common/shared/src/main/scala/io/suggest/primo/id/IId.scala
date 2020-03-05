package io.suggest.primo.id

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.10.16 18:01
  * Description: Интерфейс поля с полем id произвольного типа.
  */
trait IId[T] {

  def id: T

}


object IId {

  implicit def iId2idOpt[Id_t](x: IId[Id_t]): Option[Id_t] = Some( x.id )

}
