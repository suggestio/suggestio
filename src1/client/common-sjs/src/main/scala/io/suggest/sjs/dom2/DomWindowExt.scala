package io.suggest.sjs.dom2

import org.scalajs.dom
import org.scalajs.dom.{MediaQueryList, Navigator, Window}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName


object DomWindowExt {
  import scala.language.implicitConversions
  implicit def apply(wnd: Window): DomWindowExt =
    wnd.asInstanceOf[DomWindowExt]
}

@js.native
sealed trait DomWindowExt extends js.Object {

  @JSName("document")
  val documentU: js.UndefOr[dom.Document] = js.native

  def navigator: js.UndefOr[Navigator] = js.native
  def matchMedia(mediaQuery: String): js.UndefOr[MediaQueryList] = js.native

  var ontouchstart: js.UndefOr[js.Function1[dom.TouchEvent, _]] = js.native
  var ontouchmove: js.UndefOr[js.Function1[dom.TouchEvent, _]] = js.native
  var ontouchend: js.UndefOr[js.Function1[dom.TouchEvent, _]] = js.native
  var ontouchcancel: js.UndefOr[js.Function1[dom.TouchEvent, _]] = js.native
}
