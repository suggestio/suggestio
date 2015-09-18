package io.suggest.sc.sjs.m.msearch

import io.suggest.sjs.common.fsm.{IFsmEventMsgCompanion, IFsmMsgCompanion, IFsmMsg}
import io.suggest.sjs.common.model.CurrentTargetBackup
import org.scalajs.dom.raw.HTMLInputElement
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
  override def mtab = MTabs.Tags
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
trait IFtsFieldKeyUp extends CurrentTargetBackup {
  override type CurrentTarget_t = HTMLInputElement
}
case class FtsFieldKeyUp(override val event: KeyboardEvent)
  extends IFtsFieldKeyUp with IFsmMsg
object FtsFieldKeyUp
  extends IFsmMsgCompanion[KeyboardEvent]


/** Сигнал о попадании фокуса в input полнотекстового поиска. */
case class FtsFieldFocus(event: FocusEvent)
  extends IFsmMsg
object FtsFieldFocus
  extends IFsmMsgCompanion[FocusEvent]


/** Сигнал о потере фокуса в input'е полнотекстового поиска. */
trait IFtsFieldBlur extends CurrentTargetBackup {
  override type CurrentTarget_t = HTMLInputElement
}
case class FtsFieldBlur(override val event: FocusEvent)
  extends IFtsFieldBlur with IFsmMsg
object FtsFieldBlur
  extends IFsmMsgCompanion[FocusEvent]


/** Событие срабатываья таймера запуска поискового запроса. */
case class FtsStartRequestTimeout(generation: Long)
  extends IFsmMsg
