package io.suggest.sjs.common.controller

import io.suggest.sjs.common.model.TimeoutPromise
import org.scalajs.dom

import scala.concurrent.Promise

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.16 10:56
  * Description: Упрощенный доступ к некоторым функциям браузера.
  */
object DomQuick {

  /** Вызов setTimeout в scala-стиле.
    * Позволяет сэкономить строчки кода, возможно уменьшает скомпиленный размер.
    *
    * @param timeMs Время в миллисекундах.
    * @return id-таймера на стороне юзер-агента.
    */
  def setTimeout[U](timeMs: Double)(f: () => U): Int = {
    dom.window.setTimeout(f, timeMs)
  }

  def clearTimeout(timer: Int): Unit = {
    dom.window.clearTimeout(timer)
  }

  /** Краткий враппер над requestAnimationFrame(). */
  def requestAnimationFrame[U](f: Double => U): Int = {
    dom.window.requestAnimationFrame(f)
  }


  /** Вернуть scala Promise, который исполняется по указанному таймауту.
    *
    * @param timeMs Через сколько миллисекунд исполнить возвращаемый Promise.
    * @return Контейнер с данными, касающиеся новоиспечённого Promise'а.
    */
  def timeoutPromise(timeMs: Double): TimeoutPromise = {
    val p = Promise[Unit]()
    val timerId = setTimeout(timeMs) { () =>
      p.success(None)
    }
    TimeoutPromise(p, timerId)
  }

}
