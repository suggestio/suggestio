package io.suggest.sjs.common.ble

import io.suggest.common.radio.BeaconData
import io.suggest.common.radio.BleConstants.Beacon.Qs._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.10.16 16:51
  * Description: Модель отчёта по одному маячку.
  */
object MBleBeaconInfo {

  def toJson(m: BeaconData): js.Dictionary[js.Any] = {
    val d = js.Dictionary.empty[js.Any]

    d(UID_FN)          = m.uid
    d(DISTANCE_CM_FN)  = m.distanceCm

    d
  }

}

/** Дефолтовая реализация модели отчетов по маячкам. */
case class MBleBeaconInfo(
  override val uid          : String,
  override val distanceCm   : Int
)
  extends BeaconData
{
  override def toString: String = "B(" + uid + "," + distanceCm + "cm)"
}
