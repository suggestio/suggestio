package io.suggest.sjs.common.util

import io.suggest.common.geom.coord.MCoords2dD
import org.scalajs.dom
import org.scalajs.dom.{Touch, Window}

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.04.15 18:42
 * Description: Утиль для touch-screen задач.
 */
object TouchUtil {

  def isTouchDevice: Boolean = {
    TouchElementStub(dom.window).ontouchstart.isDefined && {
      // Для файрфокса есть проблема: если юзер хоть раз включал responsive mode в firefox dev tools, то у него будет
      // светиться ontouchstart из-за внезапной активации dom.w3c_touch_events.enabled = 1
      val ua = dom.window.navigator.userAgent
      // Юзер-агенты файрфокса https://developer.mozilla.org/en-US/docs/Web/HTTP/Gecko_user_agent_string_reference
      !ua.contains("Firefox") || {
        // Это файрфокс, и hasTouch == true. Нужно проверить, мобильник ли это.
        ua.contains("Mobile;") || ua.contains("Tablet;") || ua.contains("Fennec")
      }
    }
  }

  /** Управляется ли текущий браузер тач-скрином? */
  lazy val IS_TOUCH_DEVICE: Boolean = isTouchDevice

  /** Название события для выявления клика на touch-девайсах. */
  def EVT_NAME_TOUCH_CLICK = "touchend"

  /** Название события клика на традиционных девайсах. */
  def EVT_NAME_CLICK       = "click"

  def EVT_NAMES_MOUSE_CLICK  = List(EVT_NAME_CLICK)

  /** Название события клика для текущего девайса. */
  def clickEvtNames: List[String] = {
    if (IS_TOUCH_DEVICE) {
      List(EVT_NAME_TOUCH_CLICK)
    } else {
      EVT_NAMES_MOUSE_CLICK
    }
  }


  /**
   * @see [[http://www.bennadel.com/blog/2386-small-mistake-when-simultaneously-binding-multiple-events-with-jquery.htm]]
   * @return Список событий для jquery в виде строки event-селектора.
   */
  def clickEvtNamesJq: String = {
    clickEvtNames.mkString(" ")
  }


  def touch2coord(touch: Touch): MCoords2dD = {
    MCoords2dD(
      x = touch.pageX,
      y = touch.clientY   // TODO Это правильно? Или что это такое? В SCv2 выдаче так оно и работало...
    )
  }

}


object TouchElementStub {
  def apply(w: Window): TouchElementStub = {
    w.asInstanceOf[TouchElementStub]
  }
}
@js.native
sealed trait TouchElementStub extends js.Object {
  var ontouchstart: UndefOr[_] = js.native
}
