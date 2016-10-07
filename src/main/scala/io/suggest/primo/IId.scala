package io.suggest.primo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.10.16 18:01
  * Description: Интерфейс поля с полем id произвольного типа.
  */
trait IId[T] {

  def id: T

}
