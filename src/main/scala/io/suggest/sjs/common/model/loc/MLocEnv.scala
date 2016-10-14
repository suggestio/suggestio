package io.suggest.sjs.common.model.loc

import io.suggest.common.empty.EmptyProduct
import io.suggest.loc.LocationConstants._
import io.suggest.sjs.common.ble.MBleBeaconInfo

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.09.16 15:14
  * Description: Неявно-пустая модель данных для сервера о состоянии физического окружения текущего устройства.
  */
case class MLocEnv(
  geo           : Option[IGeoLocMin]    = None,
  bleBeacons    : Seq[MBleBeaconInfo]   = Nil
)
  extends EmptyProduct


object MLocEnv {

  def empty = MLocEnv()

  /** Сериализатор в JSON, понятный серваку. */
  def toJson(v: MLocEnv): js.Dictionary[js.Any] = {
    val d = js.Dictionary[js.Any]()

    for (g <- v.geo)
      d(GEO_LOC_FN) = IGeoLocMin.toJson(g)

    if (v.bleBeacons.nonEmpty)
      d(BLE_BEACONS_FN) = js.Array[js.Any]( v.bleBeacons.map(MBleBeaconInfo.toJson): _* )

    d
  }

}
