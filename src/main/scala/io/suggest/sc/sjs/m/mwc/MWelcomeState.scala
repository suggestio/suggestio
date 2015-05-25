package io.suggest.sc.sjs.m.mwc

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 11:01
 * Description:
 */
object MWelcomeState {

  /** Начинать скрывать карточку приветствия через указанное время. */
  def HIDE_TIMEOUT_MS = 1700
  
  /** Оценочное максимальное реальное время анимации сокрытия приветствия.
    * Через это время элемент будет считаться скрытым. */
  def FADEOUT_TRANSITION_MS = 400

}
