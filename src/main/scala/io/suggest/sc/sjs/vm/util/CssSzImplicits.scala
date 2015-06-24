package io.suggest.sc.sjs.vm.util

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.15 18:19
 * Description: Разная конвертация размеров.
 * Позволяет определить DSL для css-размеров внутри текста.
 */
trait CssSzImplicits {

  /** Заворачивание int в css-размерник. */
  implicit protected class CssSzInt(val v: Int) extends CssSzT

}


sealed trait CssSzT {
  def v: Any

  /** CSS-пиксели. */
  def px = v + "px"
}

