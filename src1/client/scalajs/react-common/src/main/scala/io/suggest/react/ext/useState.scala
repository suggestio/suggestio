package io.suggest.react.ext

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.01.2021 0:05
  * Description:
  */
@js.native
@JSImport("react", "useState")
object useState extends js.Function {

  def apply[A](init: A): js.Array[js.Any] = js.native

}
