package io.suggest.ble

import io.suggest.ble.BleConstants.Beacon.Qs._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.16 16:26
  * Description: Модель даных по одному UID-маячку.
  */
object MUidBeacon {

  /** Поддержка JSON. В первую очередь -- для нужд JS-роутера, который всё это сплющивает в URL qs. */
  implicit val MUID_BEACON_FORMAT: OFormat[MUidBeacon] = (
    (__ \ UID_FN).format[String] and
    (__ \ DISTANCE_CM_FN).format[Int]
  )(apply, unlift(unapply))

}


/**
  * Класс для инстансов модели с инфой о наблюдаемом в эфире BLE-маячке.
  *
  * @param uid Уникальный идентификатор наблюдаемого маячка:
  *            iBeacon:   "$uuid:$major:$minor"
  *            EddyStone: "$gid$bid"
  * @param distanceCm Расстояние в сантиметрах, если известно.
  */
case class MUidBeacon(
  uid          : String,
  distanceCm   : Int
) {

  override def toString = "B(" + uid + "," + distanceCm + "cm)"

}
