package io.suggest.lk.ident.center

import io.suggest.sjs.common.controller.RoutedInitController
import org.scalajs.dom
import org.scalajs.jquery.{JQueryEventObject, JQuery, jQuery}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.04.15 22:29
 * Description: Костыли от Макса для js-центровки некоторых элементов на некоторых ident-страницах.
 */

// TODO Спросить у Ильи, нужно ли это вообще, т.к. и без этого js центровка вроде бы происходит.

trait CenterContentAction extends RoutedInitController {

  private def jqWnd = jQuery(dom.window)

  /** Синхронная инициализация контроллера, если необходима. */
  override def riInit(): Unit = {
    super.riInit()
    val wnd = jqWnd
    // Если есть центруемые элементы, то при изменении размеров окна нужно повторять центровку.
    if (centerContent(wnd)) {
      wnd.resize { (e: JQueryEventObject) =>
        centerContent(jqWnd)
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
      //
    }
    hasContent
  }

}
