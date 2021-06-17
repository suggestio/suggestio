package io.suggest.radio

import io.suggest.stat.RunningAverage
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.06.18 17:45
  * Description: Модель накопленных данных по одному радио-маячку.
  */
object MRadioData {

  @inline implicit def univEq: UnivEq[MRadioData] = UnivEq.force

  def signal = GenLens[MRadioData](_.signal)
  def accuracies = GenLens[MRadioData](_.accuracies)

}


/** Состояние по одному маячку. */
case class MRadioData(
                       signal      : MRadioSignalJs,
                       accuracies  : RunningAverage[Int],
                     ) {

  /** Приведение к инстансу MBeaconSignal. */
  lazy val distanceCm = accuracies
    .average
    .map(_.toInt)

  lazy val accuracy: Option[Int] = {
    accuracies/*.stripExtemes()*/
      .average
      .map( _.toInt )
  }


  def lastDistanceCm = accuracies.queue.lastOption

}
