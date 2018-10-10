package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js


object MuiSwipeableDrawer {

  val component = JsComponent[MuiSwipeableDrawerProps, Children.Varargs, Null](Mui.SwipeableDrawer)

  def apply(props: MuiSwipeableDrawerProps)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiSwipeableDrawerProps
  extends MuiDrawerProps
{
  val disableBackdropTransition: js.UndefOr[Boolean] = js.undefined
  val disableDiscovery: js.UndefOr[Boolean] = js.undefined
  val disableSwipeToOpen: js.UndefOr[Boolean] = js.undefined
  val hysteresis: js.UndefOr[Double] = js.undefined
  val minFlingVelocity: js.UndefOr[Double] = js.undefined
  override val onClose: js.UndefOr[js.Function1[ReactEvent, Unit]]
  override val open: js.UndefOr[Boolean]
  val onOpen: js.UndefOr[js.Function1[ReactEvent, Unit]]
  val swipeAreaWidth: js.UndefOr[Double] = js.undefined
}
object MuiSwipeableDrawerProps extends MuiPropsBaseStatic[MuiSwipeableDrawerProps]

