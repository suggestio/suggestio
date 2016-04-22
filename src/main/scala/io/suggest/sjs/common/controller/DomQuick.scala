package io.suggest.sjs.common.controller

import org.scalajs.dom

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.16 10:56
  * Description: Упрощенный доступ к некоторым функциям браузера.
  */
object DomQuick {

  /** Вызов setTimeout в scala-стиле.
    * Позволяет сэкономить строчки кода, возможно уменьшает скомпиленный размер. */
  def setTimeout[U](time: Double)(f: () => U): Int = {
    dom.window.setTimeout(f, time)
  }

  def clearTimeout(timer: Int): Unit = {
    dom.window.clearTimeout(timer)
  }

}
