package io.suggest.sc.sjs.m.mdom.listen

import io.suggest.sc.sjs.m.mdom.listen.kbd.IKeyUpListener
import org.scalajs.dom.KeyboardEvent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.15 10:29
 * Description: Для удобного управления системами реакции на глобальные события используется эта модель. 
 */
object MListeners extends IKeyUpListener {
  
  /** Слушатели событий клавиатуры задаются здесь. Сама реакция происходит в DocumentCtl. */
  private var _keyboard: List[IKeyUpListener] = Nil

  def removeKeyUpListener(l: IKeyUpListener): Unit = {
    _keyboard = _keyboard.filter { _ != l }
  }

  def addKeyUpListener(l: IKeyUpListener): Unit = {
    _keyboard ::= l
  }

  override def handleKeyUp(e: KeyboardEvent): Unit = {
    _keyboard.foreach { listener =>
      listener.handleKeyUp(e)
    }
  }

}


