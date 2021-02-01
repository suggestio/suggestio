package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js


object MuiSwipeableDrawer {

  val component = JsForwardRefComponent[Props, Children.Varargs, dom.html.Div](Mui.SwipeableDrawer)

  def apply(props: Props = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)


  trait Props
    extends MuiDrawerPropsBase
  {
    val disableBackdropTransition: js.UndefOr[Boolean] = js.undefined
    val disableDiscovery: js.UndefOr[Boolean] = js.undefined
    val disableSwipeToOpen: js.UndefOr[Boolean] = js.undefined
    val hysteresis: js.UndefOr[Double] = js.undefined
    val minFlingVelocity: js.UndefOr[Double] = js.undefined
    val onClose: js.UndefOr[js.Function1[ReactEvent, Unit]]
    val onOpen: js.Function1[ReactEvent, Unit]
    val swipeAreaWidth: js.UndefOr[Double] = js.undefined
    val open: js.UndefOr[Boolean] = js.undefined
  }

}
