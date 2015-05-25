package io.suggest.sc.sjs.m.mgrid

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 16:27
 * Description: При touch-событиях нужно поддерживать и обновлять кое-какие данные по touch-событиям.
 * Тут модель занимается хранением и представлением данных touch-блокировки, чтобы подавлять ложные "клики".
 */
object MTouchLock {

  /** Инфа по текущему состоянию touch-скроллинга. */
  var isTouchLocked: Boolean = false

  def apply() = isTouchLocked
  def apply(v: Boolean): Unit = {
    isTouchLocked = v
  }

}
