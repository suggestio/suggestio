package models.mgeo

import io.suggest.common.empty.EmptyProduct
import io.suggest.model.play.qsb.{QsbSeq, QueryStringBindableImpl}
import play.api.mvc.QueryStringBindable
import io.suggest.loc.LocationConstants._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.16 19:34
  * Description: Модель описания данных физического окружения клиента выдачи s.io.
  * Нужна для объединения данных GPS, BLE-маячков, возможно ещё каких-то измерений
  * (например WIFI/BSS).
  *
  * Это неявно-пустая модель: все поля могут быть None/Nil.
  */
object MLocEnv {

  implicit def qsb(implicit
                   geoLocOptB: QueryStringBindable[Option[MGeoLoc]],
                   beaconsB  : QueryStringBindable[QsbSeq[MBleBeaconInfo]]
                  ): QueryStringBindable[MLocEnv] = {
    new QueryStringBindableImpl[MLocEnv] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MLocEnv]] = {
        val k = key1F(key)
        for {
          geoLocOptE    <- geoLocOptB.bind (k(GEO_LOC_FN),     params)
          beaconsE      <- beaconsB.bind   (k(BLE_BEACONS_FN), params)
        } yield {
          for {
            geoLocOpt   <- geoLocOptE.right
            beacons     <- beaconsE.right
          } yield {
            MLocEnv(
              geoLocOpt   = geoLocOpt,
              bleBeacons  = beacons
            )
          }
        }
      }

      override def unbind(key: String, value: MLocEnv): String = {
        _mergeUnbinded {
          val k = key1F(key)
          Iterator(
            geoLocOptB.unbind (k(GEO_LOC_FN),     value.geoLocOpt),
            beaconsB.unbind   (k(BLE_BEACONS_FN), value.bleBeacons)
          )
        }
      }
    }
  }

  def empty = apply()

}


/**
  * Класс экземпляров модели информации о локации.
  * @param geoLocOpt Данные геолокации.
  * @param bleBeacons Данные BLE-локации на основе маячков.
  */
case class MLocEnv(
  geoLocOpt     : Option[MGeoLoc]     = None,
  bleBeacons    : Seq[MBleBeaconInfo] = Nil
)
  extends EmptyProduct
