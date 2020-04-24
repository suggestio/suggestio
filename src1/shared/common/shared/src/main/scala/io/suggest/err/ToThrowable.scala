package io.suggest.err

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.04.2020 9:12
  * Description:
  */
object ToThrowable {

  implicit def dummy: ToThrowable[Throwable] =
    identity

}
