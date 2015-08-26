package io.suggest.sc.sjs.m.mwc

import io.suggest.sc.sjs.m.mfsm.{IFsmEventMsgCompanion, IFsmMsg}
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.08.15 15:29
 * Description: Сигналы в ScFsm от welcome-карточки.
 */

trait IWcStepSignal extends IFsmMsg {
  /** @return true: Пользовательский сигнал, т.е. клик мышки или что-то такое.
    *         false: автоматический сигнал, т.е. таймер.
    */
  def isUser: Boolean
}


/** Сигналы от таймеров сокрытия welcome объеденены в этот объект. */
case object WcTimeout extends IWcStepSignal {
  override def isUser = false
}


/** Клик по карточке приветствия для педалирования её сокрытия. */
case class WcClick(event: Event) extends IWcStepSignal {
  override def isUser = true
}
object WcClick
  extends IFsmEventMsgCompanion

