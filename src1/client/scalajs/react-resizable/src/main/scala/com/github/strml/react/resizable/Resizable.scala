package com.github.strml.react.resizable

import japgolly.scalajs.react.vdom.VdomNode

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import japgolly.scalajs.react.{Children, JsComponent}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.02.18 22:03
  */
object Resizable {

  val component = JsComponent[ResizableProps, Children.Varargs, js.Object]( ResizableJs )

  def apply(props: ResizableProps)(children: VdomNode*) = component(props)(children: _*)

}


@js.native
@JSImport("react-resizable", "Resizable")
object ResizableJs extends js.Object

