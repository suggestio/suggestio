package io.suggest.sc.sjs.c.cutil

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
