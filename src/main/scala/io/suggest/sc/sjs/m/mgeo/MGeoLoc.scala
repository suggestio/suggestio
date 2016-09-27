package io.suggest.sc.sjs.m.mgeo

import org.scalajs.dom.Position

import scala.scalajs.js
import io.suggest.geo.GeoConstants.GeoLocQs._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 9:50
 * Description: Модель данных о географическом местоположении на земле.
 * Модель может формироваться по данным от различных систем геопозиционирования,
 *
 * в частности от GPS, встроенных в устройства системы позиционирования по wifi/bss, или
 * по мнению самого сервера suggest.io на основе bss/wifi/ibeacon.
 * @param point Географическая координата.
 * @param accuracyM Погрешность в метрах.
 * @param timestamp Время определения геолокации.
 */
case class MGeoLoc(
  override val point        : MGeoPoint,
  override val accuracyM    : Option[Double]  = None,
  timestamp                 : Option[Long]    = None
)
  extends IGeoLocMin


object MGeoLoc {

  def apply(pos: Position): MGeoLoc = {
    MGeoLoc(
      point     = MGeoPoint(pos.coords),
      accuracyM = Some(pos.coords.accuracy),
      timestamp = Some(pos.timestamp.toLong)
    )
  }

}


/** Интерфейс минимальной модели. */
trait IGeoLocMin {
  def point       : MGeoPoint
  def accuracyM   : Option[Double]
}

/*
case class MGeoLocMin(
  override val point       : MGeoPoint,
  override val accuracyM   : Option[Double] = None
)
  extends IGeoLocMin
*/

object IGeoLocMin {

  def toJson(v: IGeoLocMin): js.Dictionary[js.Any] = {
    val d = js.Dictionary [js.Any] (
      CENTER_FN     -> v.point.toJsObject
    )

    for (accur <- v.accuracyM)
      d(ACCURACY_M_FN) = accur

    d
  }

}
