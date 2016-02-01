package io.suggest.lk.adv.direct.m

import io.suggest.lk.adv.direct.vm.nbar.tabs.CityCatTab
import io.suggest.sjs.common.fsm.IFsmMsg
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.12.15 18:24
 * Description: Сигнал о клике по группе нод.
 */
case class NgClick(
  event   : Event,
  ngHead  : CityCatTab
)
  extends IFsmMsg
