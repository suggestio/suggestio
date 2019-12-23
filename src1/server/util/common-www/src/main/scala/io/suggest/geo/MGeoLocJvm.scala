package io.suggest.geo

import io.suggest.geo.GeoConstants.GeoLocQs._
import io.suggest.xplay.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.16 10:46
  * Description: Модель данных геолокации клиентского устройства.
  * Изначально содержала только координаты центра + радиус погрешности.
  * Необходимо помнить, что юзер может вручную по карте обозначить своё присутствие,
  * тем самым вручную задав местоположение.
  */
object MGeoLocJvm {

  /** Поддержка биндинга внутри play-router. */
  implicit def mGeoLocQsb(implicit
                          geoPointB  : QueryStringBindable[MGeoPoint],
                          doubleOptB : QueryStringBindable[Option[Double]]
                         ): QueryStringBindable[MGeoLoc] = {
    new QueryStringBindableImpl[MGeoLoc] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MGeoLoc]] = {
        val k = key1F(key)
        for {
          centerE           <- geoPointB.bind (k(CENTER_FN),      params)
          accuracyOptE      <- doubleOptB.bind(k(ACCURACY_M_FN),  params)
        } yield {
          for {
            center          <- centerE
            accuracyOptM    <- accuracyOptE
          } yield {
            MGeoLoc(
              point        = center,
              accuracyOptM  = accuracyOptM
            )
          }
        }
      }

      override def unbind(key: String, value: MGeoLoc): String = {
        val k = key1F(key)
        _mergeUnbinded1(
          geoPointB .unbind (k(CENTER_FN),      value.point),
          doubleOptB.unbind (k(ACCURACY_M_FN),  value.accuracyOptM)
        )
      }
    }
  }

}

