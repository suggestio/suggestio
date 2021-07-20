package io.suggest.geo

import io.suggest.ble.MUidBeacon
import io.suggest.xplay.qsb.{QsbSeq, AbstractQueryStringBindable}
import play.api.mvc.QueryStringBindable
import io.suggest.url.bind.QueryStringBindableUtil._

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
    new AbstractQueryStringBindable[MLocEnv] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MLocEnv]] = {
        val k = key1F(key)
        val F = MLocEnv.Fields
        for {
          geoLocOptE    <- geoLocOptB.bind (k( F.GEO_LOC_FN ),     params)
          beaconsE      <- beaconsB.bind   (k( F.BEACONS_FN ), params)
        } yield {
          for {
            geoLocOpt   <- geoLocOptE
            beacons     <- beaconsE
          } yield {
            MLocEnv(
              geoLocOpt   = geoLocOpt,
              beacons  = beacons.items
            )
          }
        }
      }

      override def unbind(key: String, value: MLocEnv): String = {
        val k = key1F(key)
        val F = MLocEnv.Fields
        _mergeUnbinded1(
          geoLocOptB.unbind (k( F.GEO_LOC_FN ), value.geoLocOpt),
          beaconsB.unbind   (k( F.BEACONS_FN ), QsbSeq(value.beacons) )
        )
      }
    }
  }

}
