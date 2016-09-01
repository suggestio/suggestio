package models.mgeo

import io.suggest.model.geo.GeoPoint
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.geo.GeoConstants.GeoLocQs._
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.16 10:46
  * Description: Модель данных геолокации клиентского устройства.
  * Изначально содержала только координаты центра + радиус погрешности.
  */
object MGeoLoc {

  /** Поддержка биндинга внутри play-router. */
  implicit def qsb(implicit
                   geoPointB  : QueryStringBindable[GeoPoint],
                   doubleOptB : QueryStringBindable[Option[Double]]
                  ): QueryStringBindable[MGeoLoc] = {
    new QueryStringBindableImpl[MGeoLoc] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MGeoLoc]] = {
        val k = key1F(key)
        for {
          centerE           <- geoPointB.bind(k(CENTER_FN), params)
          accuracyOptE      <- doubleOptB.bind(k(ACCURACY_M_FN), params)
        } yield {
          for {
            center          <- centerE.right
            accuracyOptM    <- accuracyOptE.right
          } yield {
            MGeoLoc(
              center        = center,
              accuracyOptM  = accuracyOptM
            )
          }
        }
      }

      override def unbind(key: String, value: MGeoLoc): String = {
        _mergeUnbinded {
          val k = key1F(key)
          Iterator(
            geoPointB .unbind (k(CENTER_FN),      value.center),
            doubleOptB.unbind (k(ACCURACY_M_FN),  value.accuracyOptM)
          )
        }
      }
    }
  }

}


/**
  * Контейнер данных геолокации устройства.
  * @param center Центр круга, описывающего геолокацию.
  * @param accuracyOptM Радиус точности, если есть.
  */
case class MGeoLoc(
  center        : GeoPoint,
  accuracyOptM  : Option[Double] = None
)
