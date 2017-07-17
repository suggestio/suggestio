package io.suggest.ble

import io.suggest.ble.BleConstants.Beacon.Qs._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.10.16 16:51
  * Description: Модель отчёта по одному маячку.
  */
object MUidBeaconJs {

  def toJson(m: MUidBeacon): js.Dictionary[js.Any] = {
    val d = js.Dictionary.empty[js.Any]

    d(UID_FN)          = m.uid
    d(DISTANCE_CM_FN)  = m.distanceCm

    d
  }

}
