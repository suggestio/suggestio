package io.suggest.ble.beaconer.m

import io.suggest.ble.IBeaconSignal
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.06.18 17:45
  * Description: Модель накопленных данных по одному радио-маячку.
  */
object MBeaconData {

  @inline implicit def univEq: UnivEq[MBeaconData] = UnivEq.force

}


/** Состояние по одному маячку. */
case class MBeaconData(
                        // TODO beacon - Заменить на EddyStone напрямую?
                        beacon      : IBeaconSignal,
                        lastSeenMs  : Long,
                        accuracies  : BeaconAccuracyMeasurer  = new BeaconAccuracyMeasurer
                      )
