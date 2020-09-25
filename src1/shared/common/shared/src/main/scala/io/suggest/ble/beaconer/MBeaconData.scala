package io.suggest.ble.beaconer

import io.suggest.ble.BeaconDetected
import io.suggest.stat.RunningAverage
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.06.18 17:45
  * Description: Модель накопленных данных по одному радио-маячку.
  */
object MBeaconData {

  @inline implicit def univEq: UnivEq[MBeaconData] = UnivEq.force

  def beacon = GenLens[MBeaconData](_.detect)
  def accuracies = GenLens[MBeaconData](_.accuracies)

}


/** Состояние по одному маячку. */
case class MBeaconData(
                        detect      : BeaconDetected,
                        accuracies  : RunningAverage[Int],
                      ) {

  /** Приведение к инстансу MBeaconSignal. */
  lazy val distanceCm = accuracies
    .average
    .map(_.toInt)

  def lastDistanceCm = accuracies.queue.lastOption

}
