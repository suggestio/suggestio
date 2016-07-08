package io.suggest.lk.ident.center

import io.suggest.sjs.common.controller.InitRouter
import io.suggest.sjs.common.util.SafeSyncVoid
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import org.scalajs.dom
import org.scalajs.jquery.{JQueryEventObject, JQuery, jQuery}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.15 22:29
 * Description: Костыли от Макса для вертикальной js-центровки содержимого колонок на my.s.io/id страницах.
 * Без этого кода центровка происходит только по горизонтали.
 */

trait CenterContentInitRouter extends InitRouter with SafeSyncVoid {

  private def jqWnd = jQuery(dom.window)

  override protected def routeInitTarget(itg: MInitTarget): Future[_] = {
    if (itg == MInitTargets.IdentVCenterContent) {
      Future {
        ccInitSafe()
      }
    } else {
      super.routeInitTarget(itg)
    }
  }

  /** Инициализация текущего контроллер с подавлением ошибок. */
  private def ccInitSafe(): Unit = {
    _safeSyncVoid { () =>
      val wnd = jqWnd
      // Если есть центруемые элементы, то при изменении размеров окна нужно повторять центровку.
      if (centerContent(wnd)) {
        wnd.resize { (e: JQueryEventObject) =>
          centerContent(jqWnd)
        }
      }
    }
  }

  /**
   * Центровка контента по id.
   * @param wnd Окно JQuery.
   * @return true, если был обнаружен контент для центровки. Иначе false.
   */
  private def centerContent(wnd: JQuery): Boolean = {
    val content = jQuery("#centerContent")
    val hasContent = content.length > 0
    if (hasContent) {
      // копипаст из mx_cof: IndexPage.centeredContent()
      // Тут идёт текущая центровка контента.
      val minTop = 60
      val delta = 100
      val winHeight = wnd.height()
      val cntHeight = content.height()
      val diff = winHeight - cntHeight
      var top = Math.ceil(diff/2) - delta
      if (top < minTop)
        top = minTop
      content.css("padding-top", top)
    }
    hasContent
  }

}
