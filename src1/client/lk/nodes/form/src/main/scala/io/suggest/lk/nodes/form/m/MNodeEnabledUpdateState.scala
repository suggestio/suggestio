package io.suggest.lk.nodes.form.m

import diode.data.Pot
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.03.17 11:05
  * Description: Состояние обновления галочки isEnabled.
  */
object MNodeEnabledUpdateState {

  @inline implicit def univEq: UnivEq[MNodeEnabledUpdateState] = UnivEq.force

}

case class MNodeEnabledUpdateState(
                                    newIsEnabled      : Boolean,
                                    request           : Pot[_]
                                  )
