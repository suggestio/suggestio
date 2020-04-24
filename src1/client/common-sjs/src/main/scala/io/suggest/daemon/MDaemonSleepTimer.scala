package io.suggest.daemon

import io.suggest.spa.DAction
import japgolly.univeq.UnivEq

import scala.concurrent.duration.FiniteDuration

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.04.2020 14:36
  * Description: Модель настроек абстрактного фонового таймера в режиме демона.
  */
object MDaemonSleepTimer {

  @inline implicit def univEq: UnivEq[MDaemonSleepTimer] = UnivEq.derive

}

case class MDaemonSleepTimer(
                              every                 : FiniteDuration,
                              onTime                : DAction,
                              everyBoot             : Boolean               = false,
                              stopOnExit            : Boolean               = false,
                            )
