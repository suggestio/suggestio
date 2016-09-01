package models.mgeo

import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable
import io.suggest.ble.BleConstants.Beacon.Qs._
import io.suggest.util.UuidUtil

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
                   intB     : QueryStringBindable[Int],
                   strB     : QueryStringBindable[String],
                   doubleB  : QueryStringBindable[Double]
                  ): QueryStringBindable[MBleBeaconInfo] = {
    new QueryStringBindableImpl[MBleBeaconInfo] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MBleBeaconInfo]] = {
        val k = key1F(key)
        for {
          uuidStrE     <- strB.bind     (k(UUID_FN),          params)
          if uuidStrE.right.exists(UuidUtil.isUuidStrValid)
          majorE       <- intB.bind     (k(MAJOR_FN),         params)
          minorE       <- intB.bind     (k(MINOR_FN),         params)
          sigPowerE    <- doubleB.bind  (k(SIG_POWER_FN),     params)
          sigPower1mE  <- doubleB.bind  (k(SIG_POWER_1M_FN),  params)
        } yield {
          for {
            uuidStr     <- uuidStrE.right
            major       <- majorE.right
            minor       <- minorE.right
            sigPowerM   <- sigPowerE.right
            sigPower1m  <- sigPower1mE.right
          } yield {
            MBleBeaconInfo(
              uuidStr     = uuidStr,
              major       = major,
              minor       = minor,
              sigPowerM   = sigPowerM,
              sigPower1m  = sigPower1m
            )
          }
        }
      }

      override def unbind(key: String, value: MBleBeaconInfo): String = {
        _mergeUnbinded {
          val k = key1F(key)
          Iterator(
            strB.unbind(k(UUID_FN), value.uuidStr),
            intB.unbind(k(MAJOR_FN), value.major),
            intB.unbind(k(MINOR_FN), value.minor),
            doubleB.unbind(k(SIG_POWER_FN), value.sigPowerM),
            doubleB.unbind(k(SIG_POWER_1M_FN), value.sigPower1m)
          )
        }
      }
    }
  }

}

case class MBleBeaconInfo(
  uuidStr     : String,
  major       : Int,
  minor       : Int,
  sigPowerM   : Double,
  sigPower1m  : Double
)
