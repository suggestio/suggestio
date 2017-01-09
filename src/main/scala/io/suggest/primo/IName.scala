package io.suggest.primo

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.09.15 12:27
 * Description: Интерфейс моделей данных и других вещей, имеющих обязательное строковое имя.
 */
trait IName {

  /** Имя текущего элемента. */
  def name: String

}
