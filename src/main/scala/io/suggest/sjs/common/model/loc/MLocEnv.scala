package io.suggest.sjs.common.model.loc

import io.suggest.common.empty.EmptyProduct
import io.suggest.common.radio.BeaconData
import io.suggest.loc.LocationConstants._
import io.suggest.sjs.common.ble.MBleBeaconInfo

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.09.16 15:14
  * Description: Неявно-пустая модель данных для сервера о состоянии физического окружения текущего устройства.
  */
trait ILocEnv {
  def geoLocOpt     : Option[IGeoLocMin]
  def bleBeacons    : Seq[BeaconData]
}

/** Дефолтовая реализация модели-контейнера [[ILocEnv]]. */
case class MLocEnv(
  override val geoLocOpt     : Option[IGeoLocMin]    = None,
  override val bleBeacons    : Seq[BeaconData]       = Nil
)
  extends EmptyProduct
  with ILocEnv


object MLocEnv {

  def empty = MLocEnv()

  def nonEmpty(v: ILocEnv): Boolean = {
    v.geoLocOpt.nonEmpty || v.bleBeacons.nonEmpty
  }

  /** Сериализатор в JSON, понятный серваку. */
  def toJson(v: ILocEnv): js.Dictionary[js.Any] = {
    val d = js.Dictionary[js.Any]()

    for (g <- v.geoLocOpt)
      d(GEO_LOC_FN) = IGeoLocMin.toJson(g)

    if (v.bleBeacons.nonEmpty)
      d(BLE_BEACONS_FN) = js.Array[js.Any]( v.bleBeacons.map(MBleBeaconInfo.toJson): _* )

    d
  }

}
