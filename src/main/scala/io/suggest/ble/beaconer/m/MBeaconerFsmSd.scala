package io.suggest.ble.beaconer.m

import io.suggest.ble.beaconer.m.beacon.BeaconAccuracyMeasurer
import io.suggest.common.radio.BeaconSignal
import io.suggest.sjs.common.fsm.SjsFsm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:01
  * Description: Модель-контейнер state data для BeaconerFsm.
  */
case class MBeaconerFsmSd(
  watchers        : List[SjsFsm]            = Nil,
  beacons         : Map[String, BeaconSd]   = Map.empty,
  envFingerPrint  : Option[Int]            = None
) {

  // Изоляция толстых вызовов copy здесь для снижения объемов кодогенерации:

  def withWatchers(watchers2: List[SjsFsm]): MBeaconerFsmSd = {
    copy(
      watchers = watchers2
    )
  }

  def withBeacons(beacons2: Map[String, BeaconSd]): MBeaconerFsmSd = {
    copy(
      beacons = beacons2
    )
  }

  def withEnvFingerPrint(efp: Option[Int]): MBeaconerFsmSd = {
    copy(
      envFingerPrint = efp
    )
  }

}


/** Состояние по одному маячку. */
case class BeaconSd(
  beacon      : BeaconSignal,
  lastSeenMs  : Long,
  accuracies  : BeaconAccuracyMeasurer  = new BeaconAccuracyMeasurer
)
