package io.suggest.sjs.common.util

import org.scalajs.dom
import scala.scalajs.js.Object

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 18:42
 * Description: Утиль для touch-screen задач.
 */
object TouchUtil {

  /** Управляется ли текущий браузер тач-скрином? */
  lazy val isTouchDevice: Boolean = {
    Object.hasProperty(dom.window, "ontouchstart") && {
      // Для файрфокса есть проблема: если юзер хоть раз включал responsive mode в firefox dev tools, то у него будет
      // светиться ontouchstart из-за внезапной активации dom.w3c_touch_events.enabled = 1
      val ua = dom.navigator.userAgent
      // Юзер-агенты файрфокса https://developer.mozilla.org/en-US/docs/Web/HTTP/Gecko_user_agent_string_reference
      !ua.contains("Firefox") || {
        // Это файрфокс, и hasTouch == true. Нужно проверить, мобильник ли это.
        ua.contains("Mobile;") || ua.contains("Tablet;") || ua.contains("Fennec")
      }
    }
  }

  /** Название события для выявления клика на touch-девайсах. */
  def EVT_NAME_TOUCH_CLICK = "touchend"

  /** Название события клика на традиционных девайсах. */
  def EVT_NAME_CLICK       = "click"

  /** Название события клика для текущего девайса. */
  def clickEvtNames: List[String] = {
    if (isTouchDevice) {
      List(EVT_NAME_TOUCH_CLICK, EVT_NAME_CLICK)
    } else {
      List(EVT_NAME_CLICK)
    }
  }


  /**
   * @see [[http://www.bennadel.com/blog/2386-small-mistake-when-simultaneously-binding-multiple-events-with-jquery.htm]]
   * @return Список событий для jquery в виде строки event-селектора.
   */
  def clickEvtNamesJq: String = {
    clickEvtNames.mkString(" ")
  }

}
