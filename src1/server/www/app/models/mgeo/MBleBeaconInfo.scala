package models.mgeo

import io.suggest.ble.BeaconData
import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable
import io.suggest.ble.BleConstants.Beacon.Qs._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.16 12:39
  * Description: Модель данных одного обноруженного устройством BLE-маячка.
  * Часть полей совпадает с хранимой ES-моделью
  */
object MBleBeaconInfo {

  /** Поддержка биндинга инстансов модели в play router. */
  implicit def qsb(implicit
                   strB         : QueryStringBindable[String],
                   intB         : QueryStringBindable[Int]
                  ): QueryStringBindable[MBleBeaconInfo] = {

    new QueryStringBindableImpl[MBleBeaconInfo] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MBleBeaconInfo]] = {
        val k = key1F(key)
        for {
          // TODO проверять по uuid | uuid_major_minor
          uuidStrE        <- strB.bind  (k(UID_FN),          params)
          distanceCmE     <- intB.bind  (k(DISTANCE_CM_FN),  params)
        } yield {
          for {
            uuidStr       <- uuidStrE.right
            distanceCm    <- distanceCmE.right
          } yield {
            MBleBeaconInfo(
              uid         = uuidStr,
              distanceCm  = distanceCm
            )
          }
        }
      }

      override def unbind(key: String, value: MBleBeaconInfo): String = {
        _mergeUnbinded {
          val k = key1F(key)
          Iterator(
            strB.unbind (k(UID_FN),          value.uid),
            intB.unbind (k(DISTANCE_CM_FN),  value.distanceCm)
          )
        }
      }
    }
  }

}


/**
  * Класс для инстансов модели с инфой о наблюдаемом в эфире BLE-маячке.
  * @param uid Уникальный идентификатор наблюдаемого маячка:
  *            iBeacon:   "$uuid:$major:$minor"
  *            EddyStone: "$gid$bid"
  * @param distanceCm Расстояние в сантиметрах, если известно.
  */
case class MBleBeaconInfo(
  override val uid         : String,
  override val distanceCm  : Int
)
  extends BeaconData
