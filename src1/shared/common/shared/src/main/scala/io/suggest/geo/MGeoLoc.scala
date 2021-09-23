package io.suggest.geo

import io.suggest.geo.GeoConstants.GeoLocQs._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import MGeoPoint.JsonFormatters.QS_OBJECT
import diode.FastEq
import io.suggest.spa.OptFastEq
import io.suggest.text.StringUtil
import io.suggest.url.bind.{QsBindable, QsBinderF, QsUnbinderF}
import io.suggest.url.bind.QueryStringBindableUtil.{_mergeUnbinded1, key1F}
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.07.17 9:55
  * Description: Модель данных о географическом местоположении на земле.
  */

object MGeoLoc {

  /** Поддержка JSON сериализации/десериализация.
    * В первую очередь для URL query_string в js-роутере. */
  implicit def MGEO_LOC_FORMAT: OFormat[MGeoLoc] = (
    (__ \ CENTER_FN).format[MGeoPoint] and
    (__ \ ACCURACY_M_FN).formatNullable[Double]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MGeoLoc] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }


  /** Сравнивание Accuracy с отбросом неважной части расстояния. */
  object AccuracyMNearbyFastEq extends FastEq[Double] {
    override def eqv(a: Double, b: Double): Boolean = {
      // Это как бы разница не более/в районе 1 метра.
      // TODO Безопасно ли? По идее, предел int - 2,4 миллиона км, тут недосягаем.
      a.toInt ==* b.toInt
    }
  }
  /** Опциональное сравнивание accuracy в метрах с игнором неважной части расстояния. */
  lazy val AccuracyMOptNearbyFastEq = OptFastEq.Wrapped( AccuracyMNearbyFastEq )

  /** Примерное сравнивание инстансов MGeoLoc с игнором неважных долей координат и расстояния. */
  object GeoLocNearbyFastEq extends FastEq[MGeoLoc] {
    override def eqv(a: MGeoLoc, b: MGeoLoc): Boolean = {
      MGeoPoint.GeoPointsNearbyFastEq.eqv(a.point, b.point) &&
      AccuracyMOptNearbyFastEq.eqv( a.accuracyOptM, b.accuracyOptM )
    }
  }

  def point = GenLens[MGeoLoc](_.point)
  def accuracyOptM = GenLens[MGeoLoc](_.accuracyOptM)


  /** QSB support for MGeoLoc. */
  implicit def mGeoLocQsB(implicit
                          geoPointB  : QsBindable[MGeoPoint],
                          doubleOptB : QsBindable[Option[Double]],
                         ): QsBindable[MGeoLoc] = {
    new QsBindable[MGeoLoc] {
      override def bindF: QsBinderF[MGeoLoc] = { (key, params) =>
        val k = key1F(key)
        for {
          centerE           <- geoPointB.bindF (k(CENTER_FN),      params)
          accuracyOptE      <- doubleOptB.bindF(k(ACCURACY_M_FN),  params)
        } yield {
          for {
            center          <- centerE
            accuracyOptM    <- accuracyOptE
          } yield {
            MGeoLoc(
              point         = center,
              accuracyOptM  = accuracyOptM
            )
          }
        }
      }

      override def unbindF: QsUnbinderF[MGeoLoc] = { (key, value) =>
        val k = key1F(key)
        _mergeUnbinded1(
          geoPointB .unbindF (k(CENTER_FN),      value.point),
          doubleOptB.unbindF (k(ACCURACY_M_FN),  value.accuracyOptM),
        )
      }
    }
  }

}


/**
 * Модель может формироваться по данным от различных систем геопозиционирования,
 *
 * в частности от GPS, встроенных в устройства системы позиционирования по wifi/bss, или
 * по мнению самого сервера suggest.io на основе bss/wifi/ibeacon.
 * @param point Географическая координата.
 * @param accuracyOptM Погрешность в метрах.
 */

case class MGeoLoc(
                    point         : MGeoPoint,
                    accuracyOptM  : Option[Double]  = None
                  ) {

  override def toString: String = {
    StringUtil.toStringHelper(this, 16) { renderF =>
      val render0 = renderF("")
      render0( point )
      accuracyOptM foreach render0
    }
  }

}
