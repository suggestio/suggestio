package io.suggest.sc.sjs.m.msc

import io.suggest.sjs.common.fsm.IFsmMsg
import org.scalajs.dom.PopStateEvent

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.07.16 18:40
  * Description: Сигнал, контейнер события, на тему навигации по истории браузера.
  */
case class PopStateSignal(event: PopStateEvent)
  extends IFsmMsg
