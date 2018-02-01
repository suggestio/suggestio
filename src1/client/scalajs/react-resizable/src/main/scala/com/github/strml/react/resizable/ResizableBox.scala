package com.github.strml.react.resizable

import japgolly.scalajs.react.{Children, JsComponent}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.02.18 22:04
  */
object ResizableBox {

  val component = JsComponent[ResizableProps, Children.Varargs, js.Object]( ResizableJs )

}


@js.native
@JSImport("react-resizable", "ResizableBox")
object ResizableBoxJs extends js.Object
