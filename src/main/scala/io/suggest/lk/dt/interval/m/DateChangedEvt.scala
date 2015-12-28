package io.suggest.lk.dt.interval.m

import io.suggest.sjs.common.fsm.{IFsmMsg, IFsmEventMsgCompanion}
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.12.15 22:40
 * Description: Модели для fsm-сигналов событий изменения дат интервала.
 */

trait IDateChangedEvt extends IFsmMsg {

  /** Это start date? */
  def isStart: Boolean

  /** Это end date? */
  final def isEnd: Boolean = !isStart

  /** DOM-событие. */
  def event: Event

}


/** FSM-сигнал события изменения start-даты. */
case class StartChangedEvt(override val event: Event) extends IDateChangedEvt {

  override def isStart = true

}
object StartChangedEvt extends IFsmEventMsgCompanion



/** FSM-сигнал события изменения end-даты. */
case class EndChangedEvt(override val event: Event) extends IDateChangedEvt {

  override def isStart = false

}
object EndChangedEvt extends IFsmEventMsgCompanion
