package io.suggest.sc.sjs.m.mgeo

import org.scalajs.dom.Position

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
  point       : MGeoPoint,
  accuracyM   : Double,
  timestamp   : Long
)

object MGeoLoc {

  def apply(pos: Position): MGeoLoc = {
    MGeoLoc(
      point     = MGeoPoint(pos.coords),
      accuracyM = pos.coords.accuracy,
      timestamp = pos.timestamp.toLong
    )
  }

}
