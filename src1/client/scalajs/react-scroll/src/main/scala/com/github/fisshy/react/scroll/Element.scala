package com.github.fisshy.react.scroll

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.05.18 18:19
  * Description: Element component api bindings.
  */
object Element {

  val component = JsComponent[ElementProps, Children.Varargs, js.Object]( ElementJs )

  def apply(props: ElementProps)(children: VdomNode*) = component(props)(children: _*)

}


@js.native
@JSImport(REACT_SCROLL, "Element")
object ElementJs extends js.Object


trait ElementProps extends js.Object {

  val name: js.UndefOr[String] = js.undefined

  val id: js.UndefOr[String] = js.undefined

}
