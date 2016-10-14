package io.suggest.sjs.common.ble

import io.suggest.common.radio.BleConstants.Beacon.Qs._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.10.16 16:51
  * Description: Модель отчёта по одному маячку.
  */
object MBleBeaconInfo {

  def toJson(m: MBleBeaconInfo): js.Dictionary[js.Any] = {
    val d = js.Dictionary.empty[js.Any]

    d(UID_FN)          = m.beaconUid
    d(DISTANCE_CM_FN)  = m.distanceCm

    d
  }

}

/** Дефолтовая реализация модели отчетов по маячкам. */
case class MBleBeaconInfo(
  beaconUid   : String,
  distanceCm  : Int
)
