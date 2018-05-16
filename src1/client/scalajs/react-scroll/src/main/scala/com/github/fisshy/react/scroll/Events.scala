package com.github.fisshy.react.scroll

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.05.18 18:25
  * Description: Events api.
  */
@js.native
@JSImport(REACT_SCROLL, "Events")
object Events extends js.Object {

  val scrollEvent: EventStatic = js.native

}


@js.native
trait EventStatic extends js.Object {

  def register(eventName: String, f: js.Function2[js.Any, dom.html.Element, _]): Unit = js.native

  def remove(eventName: String): Unit = js.native

}


object EventsConst {

  final def BEGIN = "begin"

  final def END = "end"

}
