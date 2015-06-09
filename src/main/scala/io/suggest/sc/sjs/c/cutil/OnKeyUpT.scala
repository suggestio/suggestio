package io.suggest.sc.sjs.c.cutil

import io.suggest.sc.sjs.m.mdom.listen.MListeners
import org.scalajs.dom.KeyboardEvent
import org.scalajs.dom.ext.KeyCode

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.06.15 15:43
 * Description: Аддоны для контроллеров для упрощенного добавления фунцкий слушанья клавиатурных событий.
 */
trait KbdListenerIdT {
  protected def KBD_LISTENER_ID = getClass.getSimpleName

  /** Отключить слушалку клавиатуру в текущем контроллере. */
  protected def removeKeyUpListener(): Unit = {
    MListeners.removeKeyUpListener(KBD_LISTENER_ID)
  }
}

trait OnKeyUpT extends KbdListenerIdT {

  /** Экшен реакции на действия с клавиатуры. */
  protected def onKeyUp(e: KeyboardEvent): Unit

  /** Подключить слушалку клавиатуры в текущем контроллере. */
  protected def addKeyUpListener(): Unit = {
    MListeners.addKeyUpListener(KBD_LISTENER_ID)(onKeyUp)
  }
}


/** Полуреализация [[OnKeyUpT]] для подключения одного действия для одного набора клавиш. */
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
trait OnEscKeyUpT extends OnFilteredKeyUpT {
  /** Нажата клавиша ESC. */
  override protected def isKeyCodeHandled(keyCode: Int): Boolean = {
    keyCode == KeyCode.escape
  }
}
