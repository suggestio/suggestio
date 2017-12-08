package com.github.balloob.react.sidebar

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.17 11:57
  * Description: react-sidebar scala wrapper.
  */
object Sidebar {

  val component = JsComponent[SidebarProps, Children.Varargs, js.Object]( SidebarJs )

  def apply(props: SidebarProps)(children: VdomNode*) = component( props )(children: _*)

}


@JSImport("react-sidebar", JSImport.Default)
@js.native
object SidebarJs extends js.Object

