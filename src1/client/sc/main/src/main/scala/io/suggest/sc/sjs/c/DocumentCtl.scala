package io.suggest.sc.sjs.c

import io.suggest.sc.sjs.m.mv.MTouchLock
import io.suggest.sjs.common.controller.DomQuick
import org.scalajs.dom.TouchEvent

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
    DomQuick.setTimeout(100) { () =>
      MTouchLock(false)
    }
  }

  def onTouchCancel(e: TouchEvent): Unit = {
    MTouchLock(false)
  }

}
