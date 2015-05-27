package io.suggest.sc.sjs.m.mdom.listen

import org.scalajs.dom.KeyboardEvent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.05.15 10:29
 * Description: Для удобного управления системами реакции на глобальные события используется эта модель. 
 */
object MListeners {
  
  /** Слушатели событий клавиатуры задаются здесь. Сама реакция происходит в DocumentCtl. */
  private var _keyboard: List[(String, KeyUpListener_t)] = Nil

  def removeKeyUpListener(name: String): Unit = {
    _keyboard = _keyboard.filter { v => v._1 != name }
  }

  def addKeyUpListener(name: String)(l: KeyUpListener_t): Unit = {
    _keyboard ::= (name -> l)
  }

  def handleKeyUp(e: KeyboardEvent): Unit = {
    _keyboard.iterator.map(_._2).foreach { listener =>
      listener(e)
    }
  }

}


