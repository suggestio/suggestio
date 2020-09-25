package io.suggest.ble

import io.suggest.ble.BleConstants.Beacon.Qs._
import io.suggest.primo.id.IId
import japgolly.univeq.UnivEq
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
  implicit def MUID_BEACON_FORMAT: OFormat[MUidBeacon] = (
    (__ \ UID_FN).format[String] and
    (__ \ DISTANCE_CM_FN).formatNullable[Int]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MUidBeacon] = UnivEq.derive

}


/**
  * Класс для инстансов модели с инфой о наблюдаемом в эфире BLE-маячке.
  *
  * @param id Уникальный идентификатор наблюдаемого маячка:
  *           iBeacon:   "$uuid:$major:$minor"
  *           EddyStone: "$gid$bid"
  * @param distanceCm Расстояние в сантиметрах, если известно.
  */
final case class MUidBeacon(
                             override val id      : String,
                             distanceCm           : Option[Int]       = None,
                           )
  extends IId[String]
{
  override def toString = "B(" + id + "," + distanceCm.fold("")(_ + "cm") + ")"
}
