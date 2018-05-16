package com.github.fisshy.react.scroll

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.05.18 18:02
  * Description: scroll Link element.
  */
object Link {

  val component = JsComponent[LinkProps, Children.Varargs, js.Object]( LinkJs )

  def apply(linkProps: LinkProps)(children: VdomNode*) = component(linkProps)(children: _*)

}


@js.native
@JSImport(REACT_SCROLL, "Link")
object LinkJs extends js.Object

