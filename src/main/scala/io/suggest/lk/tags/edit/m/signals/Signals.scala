package io.suggest.lk.tags.edit.m.signals

import io.suggest.sjs.common.fsm.{IFsmMsgCompanion, IFsmMsg}
import org.scalajs.dom.Event

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
  extends IFsmMsgCompanion[Event]
