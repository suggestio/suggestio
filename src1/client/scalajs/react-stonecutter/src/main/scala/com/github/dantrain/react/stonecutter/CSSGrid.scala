package com.github.dantrain.react.stonecutter

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.11.17 18:32
  * Description: CSSGrid component.
  */
object CSSGrid {

  val component = JsComponent[CssGridProps, Children.Varargs, js.Object]( CSSGridJs )

  def apply(props: CssGridProps)(children: VdomNode*) = component(props)(children: _*)

}


/** JS component of CSSGrid. */
@js.native
@JSImport("react-stonecutter", "CSSGrid")
object CSSGridJs extends js.Object


/** Component props for using [[CSSGrid]]. */
trait CssGridProps extends PropsCommon {

  /** Animation duration in ms. Required. */
  val duration: Double

  /**
    * Animation easing function in CSS transition-timing-function format. Some Penner easings are included for convenience:
    * {{{
    *   import { easings } from 'react-stonecutter';
    *   const { quadIn, quadOut, /* ..etc. */  } = easings;
    *   Default: easings.cubicOut.
    * }}}
    */
  val easing: js.UndefOr[String] = js.undefined

}
