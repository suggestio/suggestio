package io.suggest.sc.sjs.m.mv

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 16:27
 * Description: При touch-событиях нужно поддерживать и обновлять кое-какие данные по touch-событиям.
 * Тут модель занимается хранением и представлением данных touch-блокировки, чтобы подавлять ложные "клики".
 */
object MTouchLock {

  /** Инфа по текущему состоянию touch-скроллинга. */
  private var isTouchLocked: Boolean = false

  /** Прочитать значение состояния блокировки. */
  def apply() = isTouchLocked

  /** Выставить значение touch-блокировки. */
  def apply(v: Boolean): Unit = {
    println("isTouchLocked := " + v)
    isTouchLocked = v
  }

}
