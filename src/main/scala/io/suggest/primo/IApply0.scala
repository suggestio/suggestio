package io.suggest.primo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.10.16 16:54
  * Description: Интерфейс для apply()-метода, возвращающего новые инстансы T.
  */
trait IApply0 extends TypeT {

  def apply(): T

}
