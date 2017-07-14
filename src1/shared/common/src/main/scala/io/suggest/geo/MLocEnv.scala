package io.suggest.geo

import io.suggest.ble.MBeaconData
import io.suggest.common.empty.EmptyProduct

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.07.17 11:31
  * Description: Модель описания географической и физической локации.
  */

object MLocEnv {

  val empty = apply()

}


/**
  * Класс экземпляров модели информации о локации.
  *
  * @param geoLocOpt Данные геолокации.
  * @param bleBeacons Данные BLE-локации на основе маячков.
  */
case class MLocEnv(
  geoLocOpt     : Option[MGeoLoc]     = None,
  bleBeacons    : Seq[MBeaconData]    = Nil
)
  extends EmptyProduct
