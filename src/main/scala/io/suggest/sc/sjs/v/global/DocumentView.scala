package io.suggest.sc.sjs.v.global

import io.suggest.sc.sjs.c.DocumentCtl
import io.suggest.sc.sjs.vm.SafeDoc
import org.scalajs.dom.TouchEvent
import org.scalajs.dom.raw.KeyboardEvent

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.05.15 18:09
 * Description: view документа в целом.
 */
object DocumentView {

  def initDocEvents(): Unit = {
    val ds = SafeDoc

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
    ds.addEventListener("keyup") {
      DocumentCtl.onKeyUp(_: KeyboardEvent)
    }

    //val clickEvtName = TouchUtil.clickEvtName
    // TODO document click?
  }

}
