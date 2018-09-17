package io.suggest

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.10.17 20:46
  */
package object scalaz {

  type NodePath_t  = List[Int]

  type StringValidationNel[T] = _root_.scalaz.ValidationNel[String, T]

}
