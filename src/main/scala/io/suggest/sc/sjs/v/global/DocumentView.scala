package io.suggest.sc.sjs.v.global

import io.suggest.sc.sjs.c.{ScFsm, DocumentCtl}
import io.suggest.sc.sjs.m.mfsm.signals.KbdKeyUp
import io.suggest.sc.sjs.vm.SafeDoc
import org.scalajs.dom.TouchEvent
import org.scalajs.dom.raw.KeyboardEvent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 18:09
 * Description: view документа в целом.
 */
// TODO Переписать на FSM-MVM архитектуру вместе с контроллером.
object DocumentView {

  def initDocEvents(): Unit = {
    val ds = SafeDoc
    // TODO Слать touch-lock события в ScFsm

    // Повесить события блокировки touch-событий на document.
    ds.addEventListener("touchmove") {
      DocumentCtl.onTouchMove(_: TouchEvent)
    }
    ds.addEventListener("touchend") {
      DocumentCtl.onTouchEnd(_: TouchEvent)
    }
    ds.addEventListener("touchcancel") {
      DocumentCtl.onTouchCancel(_: TouchEvent)
    }

    // Реакция на нажатия кнопок клавиатуры.
    ds.addEventListener("keyup") { event: KeyboardEvent =>
      ScFsm !! KbdKeyUp(event)
    }
  }

}
