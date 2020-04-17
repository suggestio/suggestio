package io.suggest.sc.m.in

import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.04.2020 22:24
  * Description:
  */
object MScDaemonInfo {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MScDaemonInfo] = UnivEq.derive

}


case class MScDaemonInfo(
                          // TODO таймер сканирования BLE-эфира на предмет маячков.
                        )
