package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js
import scala.scalajs.js.`|`


object MuiSnackBar {

  val component = JsComponent[MuiSnackBarProps, Children.Varargs, Null](Mui.Snackbar)

  final def apply(props: MuiSnackBarProps = MuiPropsBaseStatic.empty)(children: VdomElement*) =
    component(props)(children: _*)

}
        

/** JSON Props for [[MuiSnackBar.component]]. */
trait MuiSnackBarProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiSnackBarClasses]
{
  val action: js.UndefOr[raw.React.Node] = js.undefined
  val anchorOrigin: js.UndefOr[MuiAnchorOrigin] = js.undefined
  val autoHideDuration: js.UndefOr[Double] = js.undefined
  val ClickAwayListenerProps: js.UndefOr[js.Object] = js.undefined
  val ContentProps: js.UndefOr[js.Object] = js.undefined
  val disableWindowBlurListener: js.UndefOr[Boolean] = js.undefined
  val key: js.UndefOr[js.Any] = js.undefined
  val message: js.UndefOr[raw.React.Node] = js.undefined
  val onClose: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val onEnter: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val onEntered: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val onEntering: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val onExit: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val onExited: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val onExiting: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val open: js.UndefOr[Boolean] = js.undefined
  val resumeHideDuration: js.UndefOr[Double] = js.undefined
  val TransitionComponent: js.UndefOr[String | js.Function | js.Object] = js.undefined
  val transitionDuration: js.UndefOr[MuiTransitionDuration.TransitionDurationString_t] = js.undefined
  val TransitionProps: js.UndefOr[js.Object] = js.undefined
}


/** JSON fop [[MuiSnackBarProps.transitionDuration]]. */
trait MuiTransitionDuration extends js.Object {
  val enter: js.UndefOr[Double] = js.undefined
  val exit: js.UndefOr[Double] = js.undefined
  val appear: js.UndefOr[Double] = js.undefined
}
object MuiTransitionDuration {
  type TransitionDurationString_t <: js.Any
  type TransitionDuration_t = TransitionDurationString_t | Double | MuiTransitionDuration
  def auto = "auto".asInstanceOf[TransitionDurationString_t]
}


/** JSON css classes for [[MuiSnackBarProps.classes]]. */
trait MuiSnackBarClasses extends MuiClassesBase {
  val anchorOriginTopCenter: js.UndefOr[String] = js.undefined
  val anchorOriginBottomCenter: js.UndefOr[String] = js.undefined
  val anchorOriginTopRight: js.UndefOr[String] = js.undefined
  val anchorOriginBottomRight: js.UndefOr[String] = js.undefined
  val anchorOriginTopLeft: js.UndefOr[String] = js.undefined
  val anchorOriginBottomLeft: js.UndefOr[String] = js.undefined
}

