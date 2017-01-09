package io.suggest.common.css

import scala.language.implicitConversions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.15 18:19
 * Description: Разная конвертация размеров.
 * Позволяет определить DSL для css-размеров внутри текста.
 *
 * Поддержка реализована через universal trait + value-class в целях оптимизации.
 * @see [[http://docs.scala-lang.org/overviews/core/value-classes.html]]
 */

trait CssSzImplicits {

  /** Заворачивание int в css-размерник. */
  // TODO Сделать это через implicit value class.
  implicit protected def toCssSz(v: Int): CssSzInt = {
    new CssSzInt(v)
  }

}


sealed trait CssSzT extends Any {
  def v: Any

  /** CSS-пиксели. */
  def px = v + "px"

  def percents = v + "%"
}


// TODO Нужно сделать implicit class.
class CssSzInt(override val v: Int)
  extends AnyVal
  with CssSzT
