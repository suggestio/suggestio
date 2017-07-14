package io.suggest.geo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.07.17 9:55
  * Description: Модель данных о географическом местоположении на земле.
  */


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
