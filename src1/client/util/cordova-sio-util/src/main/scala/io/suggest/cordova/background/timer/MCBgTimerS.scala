package io.suggest.cordova.background.timer

import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.04.2020 14:48
  * Description: Модель состояния таймера cordova-background.
  */
object MCBgTimerS {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MCBgTimerS] = UnivEq.derive

}


/** Состояние таймера.
  *
  * НЕ ясно, что тут хранить, т.к. таймер живёт вне одного запуска приложения.
  * В любой момент состояние таймера точно неизвестно.
  */
case class MCBgTimerS(
                     )
