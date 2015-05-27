package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.m.mdom.listen.MListeners
import io.suggest.sc.sjs.m.mv.MTouchLock
import org.scalajs.dom
import org.scalajs.dom.TouchEvent
import org.scalajs.dom.raw.KeyboardEvent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 17:11
 * Description: Контроллер событий уровня документа.
 */
object DocumentCtl {

  def onTouchMove(e: TouchEvent): Unit = {
    if (!MTouchLock())
      MTouchLock(true)
  }

  def onTouchEnd(e: TouchEvent): Unit = {
    dom.setTimeout(
      { () => MTouchLock(false) },
      100
    )
  }

  def onTouchCancel(e: TouchEvent): Unit = {
    MTouchLock(false)
  }

  /** Обработка событий клавиатуры: отпускание клавиш. */
  def onKeyUp(e: KeyboardEvent): Unit = {
    MListeners.handleKeyUp(e)
  }

}
