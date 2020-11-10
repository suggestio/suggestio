package com.github.strml.react.resizable

import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.{Children, JsComponent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.02.18 22:04
  */
object ResizableBox {

  val component = JsComponent[ResizableBoxProps, Children.Varargs, js.Object]( ResizableJs )

  def apply(props: ResizableBoxProps)(children: VdomNode*) = component(props)(children: _*)

}


@js.native
@JSImport("react-resizable", "ResizableBox")
object ResizableBoxJs extends js.Object
