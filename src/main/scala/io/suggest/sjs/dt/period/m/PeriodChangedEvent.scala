package io.suggest.sjs.dt.period.m

import io.suggest.sjs.dt.period.vm.PeriodVm
import io.suggest.sjs.common.fsm.{IFsmEventMsgCompanion, IFsmMsg}
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLSelectElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 17:33
 * Description: Модель сообщений об изменении периода дат.
 */
case class PeriodChangedEvent(event: Event)
  extends IFsmMsg {

  lazy val vm = PeriodVm( event.target.asInstanceOf[HTMLSelectElement] )
}

object PeriodChangedEvent
  extends IFsmEventMsgCompanion
