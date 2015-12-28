package io.suggest.lk.dt.interval.m

import io.suggest.sjs.common.fsm.{IFsmEventMsgCompanion, IFsmMsg}
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 17:33
 * Description: Модель сообщений об изменении периода дат.
 */
case class PeriodChangedEvent(event: Event)
  extends IFsmMsg
object PeriodChangedEvent
  extends IFsmEventMsgCompanion
