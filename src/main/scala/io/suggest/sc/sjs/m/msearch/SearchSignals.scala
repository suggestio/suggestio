package io.suggest.sc.sjs.m.msearch

import io.suggest.sc.sjs.m.mfsm.{IFsmMsgCompanion, IFsmEventMsgCompanion, IFsmMsg}
import org.scalajs.dom.{FocusEvent, KeyboardEvent, Event}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 16:52
 * Description: Сообщения для FSM, связанные с поиском.
 */
trait ITabClickSignal extends IFsmMsg {
  /** Тело события. */
  def event: Event
  /** Таб, к которому относится произошедшее событие. */
  def mtab: MTab
}
trait ITabClickSignalCompanion
  extends IFsmEventMsgCompanion


/** Сообщение о клике по кнопке вкладки хеш-тегов. */
case class STabBtnHtagsClick(override val event: Event)
  extends ITabClickSignal {
  override def mtab = MTabs.HashTags
}
object STabBtnHtagsClick
  extends ITabClickSignalCompanion


case class STabBtnGeoClick(override val event: Event)
  extends ITabClickSignal {
  override def mtab = MTabs.Geo
}
object STabBtnGeoClick
  extends ITabClickSignalCompanion



/** Сигнал о наборе в полнотекстового поиска. */
case class FtsFieldKeyUp(event: KeyboardEvent)
  extends IFsmMsg
object FtsFieldKeyUp
  extends IFsmMsgCompanion[KeyboardEvent]


/** Сигнал о попадании фокуса в input полнотекстового поиска. */
case class FtsFieldFocus(event: FocusEvent)
  extends IFsmMsg
object FtsFieldFocus
  extends IFsmMsgCompanion[FocusEvent]


/** Сигнал о потере фокуса в input'е полнотекстового поиска. */
case class FtsFieldBlur(event: FocusEvent)
  extends IFsmMsg
object FtsFieldBlur
  extends IFsmMsgCompanion[FocusEvent]


/** Событие срабатываья таймера запуска поискового запроса. */
case class FtsStartRequestTimeout(generation: Long)
  extends IFsmMsg
