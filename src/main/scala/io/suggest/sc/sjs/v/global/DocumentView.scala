package io.suggest.sc.sjs.v.global

import io.suggest.sc.sjs.c.DocumentCtl
import io.suggest.sc.sjs.m.SafeDoc
import org.scalajs.dom.TouchEvent

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

    // TODO Нужна реакция на нажатия кнопок Esc, Left, Right и т.д., которые будут пробрасываться в необходимый handler.
    //val clickEvtName = TouchUtil.clickEvtName
    // TODO document click?
  }

}
