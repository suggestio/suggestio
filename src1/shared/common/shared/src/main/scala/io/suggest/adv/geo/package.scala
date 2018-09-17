package io.suggest.adv

import io.suggest.adv.rcvr.RcvrKey
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.12.16 15:22
  */
package object geo {

  type RcvrsMap_t = Map[RcvrKey, Boolean]

  @inline implicit def rcvrsMapUe: UnivEq[RcvrsMap_t] = UnivEq.force

}
