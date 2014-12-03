package io.suggest.model

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.12.14 12:20
 * Description: Утиль для scala Enumeration.
 */

/** Добавить неявный конвертер value к типу Enum'а. */
trait EnumValue2Val extends Enumeration {
  type T

  implicit def value2val(x: Value): T = x.asInstanceOf[T]
}


/** Добавить метод value2val к scala enum'у. */
trait EnumMaybeWithName extends EnumValue2Val {

  def maybeWithName(n: String): Option[T] = {
    values
      .find(_.toString == n)
      .map(value2val)
  }

}
