package io.suggest.ble

import io.suggest.ble.BleConstants.Beacon.Qs._
import io.suggest.xplay.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.16 12:39
  * Description: Модель данных одного обноруженного устройством BLE-маячка.
  * Часть полей совпадает с хранимой ES-моделью
  */
object MBeaconDataJvm {

  /** Поддержка биндинга инстансов модели в play router. */
  implicit def mBeaconDataQsb(implicit
                              strB         : QueryStringBindable[String],
                              intB         : QueryStringBindable[Int]
                             ): QueryStringBindable[MUidBeacon] = {

    new QueryStringBindableImpl[MUidBeacon] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MUidBeacon]] = {
        val k = key1F(key)
        for {
          // TODO проверять по uuid | uuid_major_minor
          uuidStrE        <- strB.bind  (k(UID_FN),          params)
          distanceCmE     <- intB.bind  (k(DISTANCE_CM_FN),  params)
        } yield {
          for {
            uuidStr       <- uuidStrE
            distanceCm    <- distanceCmE
          } yield {
            MUidBeacon(
              id         = uuidStr,
              distanceCm  = distanceCm
            )
          }
        }
      }

      override def unbind(key: String, value: MUidBeacon): String = {
        val k = key1F(key)
        _mergeUnbinded1(
          strB.unbind (k(UID_FN),          value.id),
          intB.unbind (k(DISTANCE_CM_FN),  value.distanceCm)
        )
      }
    }
  }

}
