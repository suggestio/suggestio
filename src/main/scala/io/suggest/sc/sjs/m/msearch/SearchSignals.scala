package io.suggest.sc.sjs.m.msearch

import io.suggest.sc.sjs.m.mfsm.{IFsmEventMsgCompanion, IFsmMsg}
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 16:52
 * Description: Сообщения для FSM, связанные с поиском.
 */
trait ITabClickSignal extends IFsmMsg {
  def event: Event
}
trait ITabClickSignalCompanion
  extends IFsmEventMsgCompanion


/** Сообщение о клике по кнопке вкладки хеш-тегов. */
case class HtagsTabBtnClick(override val event: Event)
  extends ITabClickSignal
object HtagsTabBtnClick
  extends ITabClickSignalCompanion
