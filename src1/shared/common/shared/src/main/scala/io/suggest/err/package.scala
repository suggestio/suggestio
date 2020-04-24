package io.suggest

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.04.2020 9:52
  */
package object err {

  type ToThrowable[From] = From => Throwable

}
