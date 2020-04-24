package io.suggest.err

import scala.scalajs.js
import scala.scalajs.js.JavaScriptException

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.04.2020 9:14
  * Description: js-поддержка для ToThrowable-typeclass'ов
  */
object ToThrowableJs {

  /** Поддержка JavaScriptException. */
  implicit def jsAnyToThrow: ToThrowable[js.Any] =
    JavaScriptException

}
