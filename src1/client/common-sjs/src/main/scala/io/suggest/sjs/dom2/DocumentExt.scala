package io.suggest.sjs.dom2

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

@js.native
trait DocumentExt extends js.Object {

  @JSName("getElementById")
  val getElementByIdU: js.UndefOr[js.Function1[String, dom.Element]] = js.native

}

