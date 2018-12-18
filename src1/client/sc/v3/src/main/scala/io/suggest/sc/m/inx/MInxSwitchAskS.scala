package io.suggest.sc.m.inx

import diode.FastEq
import io.suggest.maps.nodes.MGeoNodesResp
import io.suggest.sc.m.HandleScApiResp
import io.suggest.sc.v.search.SearchCss
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.11.18 17:57
  * Description: Модель диалога подтверждения авто-переключения выдачи на новую локацию.
  */
object MInxSwitchAskS {

  implicit object MInxSwitchAskSFastEq extends FastEq[MInxSwitchAskS] {
    override def eqv(a: MInxSwitchAskS, b: MInxSwitchAskS): Boolean = {
      (a.okAction  ===* b.okAction) &&
      (a.nodesResp ===* b.nodesResp) &&
      (a.searchCss ===* b.searchCss)
    }
  }

  @inline implicit def univEq: UnivEq[MInxSwitchAskS] = UnivEq.derive

}


case class MInxSwitchAskS(
                           okAction     : HandleScApiResp,
                           nodesResp    : MGeoNodesResp,
                           searchCss    : SearchCss,
                         )
