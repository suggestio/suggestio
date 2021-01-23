package io.suggest.react

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.01.2021 0:08
  */
package object ext {

  /** scala-wrapper for useState(). */
  def useStateF[A](init: A): (A, js.Function1[A, Unit]) = {
    val arr = useState(init)
    val currValue = arr(0).asInstanceOf[A]
    val setValue = arr(1).asInstanceOf[js.Function1[A, Unit]]
    (currValue, setValue)
  }

}
