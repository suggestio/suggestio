package io.suggest.geo

import io.suggest.ble.MUidBeacon
import io.suggest.common.empty.EmptyProduct
import io.suggest.loc.LocationConstants._
import io.suggest.model.play.qsb.{QsbSeq, QueryStringBindableImpl}
import play.api.mvc.QueryStringBindable

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
object MLocEnvJvm {

  implicit def mLocEnvQsb(implicit
                          geoLocOptB: QueryStringBindable[Option[MGeoLoc]],
                          beaconsB  : QueryStringBindable[QsbSeq[MUidBeacon]]
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
        val k = key1F(key)
        _mergeUnbinded1(
          geoLocOptB.unbind (k(GEO_LOC_FN),     value.geoLocOpt),
          beaconsB.unbind   (k(BLE_BEACONS_FN), value.bleBeacons)
        )
      }
    }
  }

}
