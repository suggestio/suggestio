package io.suggest.sjs.dom2

import io.suggest.sjs.common.model.TimeoutPromise
import org.scalajs.dom

import scala.concurrent.Promise
import scala.scalajs.js
import scala.scalajs.js.Date

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.16 10:56
  * Description: Упрощенный доступ к некоторым функциям браузера.
  *
  * timeMs - только Int, т.к. есть риск ошибится с неявной Long/Double-конверсией, приводящей к проблемам в Safari.
  */
object DomQuick {

  /** Принудительная перезагрузка страницы. */
  def reloadPage(): Unit = {
    println("Reloading page: " + Thread.currentThread().getStackTrace.to(LazyList).tail.take(3).mkString("\n"))
    dom.document.location.reload(true)
  }

  /** Перейти по указанной ссылке. */
  def goToLocation(url: String): Unit = {
    dom.document.location.assign(url)
  }

  /** Вернуть scala Promise, который исполняется по указанному таймауту.
    *
    * @param timeMs Через сколько миллисекунд исполнить возвращаемый Promise.
    * @return Контейнер с данными, касающиеся новоиспечённого Promise'а.
    */
  def timeoutPromise(timeMs: Double): TimeoutPromise[None.type] = {
    timeoutPromiseT(timeMs)(None)
  }
  def timeoutPromiseT[T](timeMs: Double)(res: T): TimeoutPromise[T] = {
    val p = Promise[T]()
    val timerId = js.timers.setTimeout(timeMs) {
      p.success(res)
    }
    TimeoutPromise(p, timerId)
  }


  /** Описать текущий сдвиг времени в минутах. */
  def tzOffsetMinutes: Int = new Date().getTimezoneOffset().toInt

  /** Текущий год. */
  def currentYear = new Date().getFullYear()

}
