package io.suggest.primo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.16 13:33
  * Description: Интерфейс к часто-используемому полю strId: String.
  */
trait IStrId {

  /** Некий строковой ключ. Например, ключ элемента модели. */
  def strId: String

  override def toString: String = strId

}
