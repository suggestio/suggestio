package io.suggest.sc.sjs.m.mhdr

import io.suggest.sjs.common.fsm.signals.IMenuBtnClick
import io.suggest.sjs.common.fsm.{IFsmEventMsgCompanion, IFsmMsg}
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.15 16:54
 * Description: Различные сообщения в основной FSM от vm'ок заголовка.
 */

/** Сигнал о клике по кнопке открытия панели поиска. */
case class ShowSearchClick(e: Event) extends IFsmMsg
object ShowSearchClick extends IFsmEventMsgCompanion


/** Сигнал о клике по кнопке сокрытия панели поиска. */
case class HideSearchClick(e: Event) extends IFsmMsg
object HideSearchClick extends IFsmEventMsgCompanion


/** Сигнал о клике по кнопке отображения index'а текущей выдачи. */
case class ShowIndexClick(e: Event) extends IFsmMsg
object ShowIndexClick extends IFsmEventMsgCompanion


/** Сигнал о клике по кнопке открытия nav-панели. */
case class ShowNavClick(e: Event) extends IMenuBtnClick {
  override def isOpenMenu = Some(true)
}
object ShowNavClick extends IFsmEventMsgCompanion


/** Сигнал о клике по кнопке сокрытия панели навигации. */
case class HideNavClick(e: Event) extends IMenuBtnClick {
  override def isOpenMenu = Some(false)
}
object HideNavClick extends IFsmEventMsgCompanion


/** Сигнал о клике по кнопке возврата на предыдущий узел. */
case class PrevNodeBtnClick(e: Event) extends IFsmMsg
object PrevNodeBtnClick extends IFsmEventMsgCompanion


/** Клик по логотипу узла в заголовке.
  * Не относится к focused-выдаче. */
case class LogoClick(e: Event) extends IFsmMsg
object LogoClick extends IFsmEventMsgCompanion
