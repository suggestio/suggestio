package io.suggest.sc.sjs.m.mhdr

import io.suggest.sc.sjs.m.mfsm.{IFsmEventMsgCompanion, IFsmMsg}
import org.scalajs.dom.Event

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.08.15 16:54
 * Description: Различные сообщения в основной FSM от vm'ок заголовка.
 */

/** Сообщение о клике по кнопке открытия панели поиска. */
case class ShowSearchClick(e: Event) extends IFsmMsg
object ShowSearchClick extends IFsmEventMsgCompanion


/** Сообщение о клике по кнопке сокрытия панели поиска. */
case class HideSearchClick(e: Event) extends IFsmMsg
object HideSearchClick extends IFsmEventMsgCompanion


/** Сообщение о клике по кнопке отображения index'а текущей выдачи. */
case class ShowIndexClick(e: Event) extends IFsmMsg
object ShowIndexClick extends IFsmEventMsgCompanion


/** Сообщение о клике по кнопке открытия nav-панели. */
case class ShowNavClick(e: Event) extends IFsmMsg
object ShowNavClick extends IFsmEventMsgCompanion


/** Сообщение о клике по кнопке сокрытия панели навигации. */
case class HideNavClick(e: Event) extends IFsmMsg
object HideNavClick extends IFsmEventMsgCompanion
