package io.suggest.lk.tags.edit.m.signals

import io.suggest.lk.tags.edit.vm.search.hints.SRow
import io.suggest.sjs.common.fsm.{IFsmEventMsgCompanion, IFsmMsg, IFsmMsgCompanion}
import org.scalajs.dom.{Event, FocusEvent, KeyboardEvent}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.09.15 16:47
 * Description: Сигналы, передаваемый в TagsEditFsm.
 */

trait ITagsEditFsmSignal
  extends IFsmMsg


/** Сигнал о нажатии кнопки добавления тега. */
case class AddBtnClick(event: Event)
  extends ITagsEditFsmSignal
object AddBtnClick
  extends IFsmEventMsgCompanion


/** Сигнал о получение фокуса ввода в поле ввода названия тега. */
case class NameInputFocus(event: FocusEvent)
  extends ITagsEditFsmSignal
object NameInputFocus
  extends IFsmMsgCompanion[FocusEvent]


/** Сигнал о расфокусировке поля ввода имени тега. */
case class NameInputBlur(event: FocusEvent)
  extends ITagsEditFsmSignal
object NameInputBlur
  extends IFsmMsgCompanion[FocusEvent]


/** Сигнал о вводе в поле ввода имени тега. */
case class NameInputEvent(event: Event)
  extends ITagsEditFsmSignal
object NameInputEvent
  extends IFsmEventMsgCompanion

/** Сигнал о вводе с клавиатуры имени тега. */
case class TagNameTyping(event: KeyboardEvent)
  extends ITagsEditFsmSignal
object TagNameTyping
  extends IFsmMsgCompanion[KeyboardEvent]


/** Сабмит имени тега. */
case class NameInputSubmit(event: KeyboardEvent)
  extends ITagsEditFsmSignal
object NameInputSubmit
  extends IFsmMsgCompanion[KeyboardEvent]


/** Сработал таймер для начала запроса поиска тегов по имени. */
case class StartSearchTimer(ts: Long)
  extends ITagsEditFsmSignal


/** Клик по кнопке удаления тега. */
case class DeleteClick(event: Event)
  extends ITagsEditFsmSignal
object DeleteClick
  extends IFsmEventMsgCompanion


/** Юзер выбрал в списке найденных тегов какую-то строку. */
case class TagFoundClick(srow: SRow, event: Event)
  extends ITagsEditFsmSignal
