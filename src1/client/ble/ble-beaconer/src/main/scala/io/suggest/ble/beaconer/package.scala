package io.suggest.ble

import diode.Effect

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.04.2020 10:29
  * Description: Типы для beaconer'а.
  */
package object beaconer {

  type BeaconsNearby_t = Seq[MUidBeacon]

  type OnNearbyChangeF = (BeaconsNearby_t, BeaconsNearby_t) => Option[Effect]

}
