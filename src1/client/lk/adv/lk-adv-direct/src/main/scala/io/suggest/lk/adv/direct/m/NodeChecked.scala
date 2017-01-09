package io.suggest.lk.adv.direct.m

import io.suggest.lk.adv.direct.vm.nbar.nodes.NodeCheckBox
import io.suggest.sjs.common.fsm.IFsmMsg
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.01.16 16:39
 * Description: Сигнал для FSM о клике по ноде в списке нод.
 */
case class NodeChecked(
  ncb   : NodeCheckBox,
  event : Event
)
  extends IFsmMsg
