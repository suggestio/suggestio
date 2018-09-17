package io.suggest.ueq

import io.suggest.sjs.leaflet.map.LMap
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.06.18 13:10
  */
object MapsUnivEq {

  @inline implicit def lMapUe: UnivEq[LMap] = UnivEq.force

}
