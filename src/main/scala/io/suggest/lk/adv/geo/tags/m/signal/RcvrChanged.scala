package io.suggest.lk.adv.geo.tags.m.signal

import io.suggest.lk.adv.geo.tags.vm.popup.rcvr.NodeDiv
import io.suggest.sjs.common.fsm.IFsmMsg

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.12.16 22:00
  * Description: Сигнал об изменении конфигурации узлов-ресиверов на карте.
  */
case class RcvrChanged(ncb: NodeDiv, rcvrId: String)
  extends IFsmMsg
