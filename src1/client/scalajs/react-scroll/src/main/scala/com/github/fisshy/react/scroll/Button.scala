package com.github.fisshy.react.scroll

import japgolly.scalajs.react._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.05.18 19:06
  * Description: input-based implementation of scroll-link.
  */
object Button {

  val component = JsComponent[LinkProps, Children.None, js.Object]( ButtonJs )

  def apply(linkProps: LinkProps) = component(linkProps)

}


@js.native
@JSImport(REACT_SCROLL, "Button")
object ButtonJs extends js.Object
