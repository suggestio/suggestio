package io.suggest.scalaz

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.07.17 21:45
  * Description: Интерфейс для унифицированного запуска статической валидации.
  */
trait IValidateDataT[T] {

  protected def doValidation(v: T): StringValidationNel[T]

  def validateData(mf: T) = doValidation(mf).toEither

}

