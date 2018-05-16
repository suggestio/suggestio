package com.github.fisshy.react.scroll

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.05.18 22:42
  * Description: scroller.js API.
  */

@JSImport(REACT_SCROLL, "scroller")
@js.native
object Scroller extends js.Object {

  def unmount(): Unit = js.native

  def register(name: String, element: dom.html.Element): Unit = js.native

  def unregister(name: String): Unit = js.native

  def get(name: String): dom.html.Element = js.native

  def scrollTo(to: String,
               options: LinkProps = js.native): Unit = js.native

}
