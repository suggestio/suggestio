package io.suggest.geo

import io.suggest.geo.GeoConstants.GeoLocQs._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import MGeoPoint.Implicits.MGEO_POINT_FORMAT_QS_OBJECT

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.07.17 9:55
  * Description: Модель данных о географическом местоположении на земле.
  */

object MGeoLoc {

  /** Поддержка JSON сериализации/десериализация.
    * В первую очередь для URL query_string в js-роутере. */
  implicit val MGEO_LOC_FORMAT: OFormat[MGeoLoc] = (
    (__ \ CENTER_FN).format[MGeoPoint] and
    (__ \ ACCURACY_M_FN).formatNullable[Double]
  )(apply, unlift(unapply))

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
                  )
