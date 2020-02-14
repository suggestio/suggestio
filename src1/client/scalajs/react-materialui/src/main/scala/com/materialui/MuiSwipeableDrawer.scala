package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js


object MuiSwipeableDrawer {

  val component = JsComponent[MuiSwipeableDrawerProps, Children.Varargs, Null](Mui.SwipeableDrawer)

  def apply(props: MuiSwipeableDrawerProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiSwipeableDrawerProps
  extends MuiDrawerPropsBase
  with MuiModalPropsBase
{
  val disableBackdropTransition: js.UndefOr[Boolean] = js.undefined
  val disableDiscovery: js.UndefOr[Boolean] = js.undefined
  val disableSwipeToOpen: js.UndefOr[Boolean] = js.undefined
  val hysteresis: js.UndefOr[Double] = js.undefined
  val minFlingVelocity: js.UndefOr[Double] = js.undefined
  val onClose: js.UndefOr[js.Function1[ReactEvent, Unit]]
  val onOpen: js.Function1[ReactEvent, Unit]
  val swipeAreaWidth: js.UndefOr[Double] = js.undefined
}

