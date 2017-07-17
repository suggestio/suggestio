package io.suggest.geo

import io.suggest.ble.MUidBeacon
import io.suggest.common.empty.EmptyProduct
import io.suggest.loc.LocationConstants._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.07.17 11:31
  * Description: Модель описания географической и физической локации.
  */

object MLocEnv {

  val empty = apply()


  /** Поддержка JSON для инстансов [[MLocEnv]].
    * В первую очередь для js-роутера и qs.
    */
  implicit val MLOC_ENV_FORMAT: OFormat[MLocEnv] = (
    (__ \ GEO_LOC_FN).formatNullable[MGeoLoc] and
    (__ \ BLE_BEACONS_FN).formatNullable[Seq[MUidBeacon]]
      .inmap[Seq[MUidBeacon]](
        { optSeq => optSeq.getOrElse(Nil) },
        { seq => if (seq.isEmpty) None else Some(seq) }
      )
  )(apply, unlift(unapply))

}


/**
  * Класс экземпляров модели информации о локации.
  *
  * @param geoLocOpt Данные геолокации.
  * @param bleBeacons Данные BLE-локации на основе маячков.
  */
case class MLocEnv(
  geoLocOpt     : Option[MGeoLoc]    = None,
  bleBeacons    : Seq[MUidBeacon]    = Nil
)
  extends EmptyProduct
