package com.github.souporserious.react.measure

import org.scalajs.dom.html
import org.scalajs.dom.raw.ClientRect

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.2019 12:04
  * @see [[https://github.com/que-etc/resize-observer-polyfill/blob/master/src/ResizeObserverEntry.js]]
  */
/*
@JSImport("resize-observer-polyfill", "ResizeObserverEntry")
class ResizeObserverEntry(
                           val target   : html.Element,
                           rectInit     : ClientRect,
                         )
*/
@js.native
trait ResizeObserverEntry extends js.Object {

  val target   : html.Element = js.native

  def contentRect: ClientRect = js.native

}
