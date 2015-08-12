package io.suggest.sc.sjs.c.cutil

import org.scalajs.dom.KeyboardEvent
import org.scalajs.dom.ext.KeyCode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.06.15 15:43
 * Description: Аддоны для контроллеров для упрощенного добавления фунцкий слушанья клавиатурных событий.
 */
@deprecated("FSM-MVM: Use ScFsmStub.FsmState._onKbdKeyUp()", "2015.aug.12")
trait KbdListenerIdT {
  protected def KBD_LISTENER_ID = getClass.getSimpleName

  /** Отключить слушалку клавиатуру в текущем контроллере. */
  protected def removeKeyUpListener(): Unit = {
  }
}


@deprecated("FSM-MVM: Use ScFsmStub.FsmState._onKbdKeyUp()", "2015.aug.12")
trait OnKeyUpT extends KbdListenerIdT {

  /** Экшен реакции на действия с клавиатуры. */
  protected def onKeyUp(e: KeyboardEvent): Unit

  /** Подключить слушалку клавиатуры в текущем контроллере. */
  protected def addKeyUpListener(): Unit = {
  }
}


/** Полуреализация [[OnKeyUpT]] для подключения одного действия для одного набора клавиш. */
@deprecated("FSM-MVM: Use ScFsmStub.FsmState._onKbdKeyUp()", "2015.aug.12")
trait OnFilteredKeyUpT extends OnKeyUpT {

  /** @return true, если клавиша будет передана в onFilteredKeyUp().
    *         false, если клавишу этот контроллер должен проигнорить. */
  protected def isKeyCodeHandled(keyCode: Int): Boolean

  /** Нажата ожидаемая клавиша. */
  protected def onFilteredKeyUp(e: KeyboardEvent): Unit

  /** Экшен реакции на действия с клавиатуры. */
  override protected def onKeyUp(e: KeyboardEvent): Unit = {
    if (isKeyCodeHandled(e.keyCode)) {
      onFilteredKeyUp(e)
    }
  }

}


/** Частоиспользуемая обработка только клавиши клавиатуры ESC. */
@deprecated("FSM-MVM: Use ScFsmStub.FsmState._onKbdKeyUp()", "2015.aug.12")
trait OnEscKeyUpT extends OnFilteredKeyUpT {
  /** Нажата клавиша ESC. */
  override protected def isKeyCodeHandled(keyCode: Int): Boolean = {
    keyCode == KeyCode.escape
  }
}
