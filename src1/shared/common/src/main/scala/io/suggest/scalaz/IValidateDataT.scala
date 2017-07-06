package io.suggest.scalaz

import scalaz.ValidationNel

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.07.17 21:45
  * Description: Интерфейс для унифицированного запуска статической валидации.
  */
trait IValidateDataT[T] {

  protected def doValidation(v: T): ValidationNel[String, T]

  def validateData(mf: T) = doValidation(mf).toEither

}

