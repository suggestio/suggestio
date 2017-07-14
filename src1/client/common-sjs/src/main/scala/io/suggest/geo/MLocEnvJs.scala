package io.suggest.geo

import io.suggest.ble.MBeaconDataJs
import io.suggest.loc.LocationConstants._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.09.16 15:14
  * Description: Неявно-пустая модель данных для сервера о состоянии физического окружения текущего устройства.
  */

object MLocEnvJs {

  /** Сериализатор в JSON, понятный серваку. */
  def toJson(v: MLocEnv): js.Dictionary[js.Any] = {
    val d = js.Dictionary[js.Any]()

    for (g <- v.geoLocOpt)
      d(GEO_LOC_FN) = MGeoLocJs.toJson(g)

    if (v.bleBeacons.nonEmpty)
      d(BLE_BEACONS_FN) = js.Array[js.Any]( v.bleBeacons.map(MBeaconDataJs.toJson): _* )

    d
  }

}
