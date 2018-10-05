package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js
import scala.scalajs.js.`|`


object MuiSnackBar {

  val component = JsComponent[MuiSnackBarProps, Children.Varargs, Null](Mui.Snackbar)

  def apply(props: MuiSnackBarProps = MuiSnackBarProps.empty)(children: VdomElement*) =
    component(props)(children: _*)

}
        

/** JSON Props for [[MuiSnackBar.component]]. */
trait MuiSnackBarProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiSnackBarClasses]
{
  val action: js.UndefOr[React.Node] = js.undefined
  val anchorOrigin: js.UndefOr[MuiSnackBarAnchorOrigin] = js.undefined
  val autoHideDuration: js.UndefOr[Double] = js.undefined
  val ClickAwayListenerProps: js.UndefOr[js.Object] = js.undefined
  val ContentProps: js.UndefOr[js.Object] = js.undefined
  val disableWindowBlurListener: js.UndefOr[Boolean] = js.undefined
  val key: js.UndefOr[js.Any] = js.undefined
  val message: js.UndefOr[React.Node] = js.undefined
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
  val transitionDuration: js.UndefOr[Double | MuiSnackBarTransitionDuration] = js.undefined
  val TransitionProps: js.UndefOr[js.Object] = js.undefined
}
object MuiSnackBarProps extends MuiPropsBaseStatic[MuiSnackBarProps]


/** JSON fop [[MuiSnackBarProps.anchorOrigin]]. */
trait MuiSnackBarAnchorOrigin extends js.Object {
  val vertical: js.UndefOr[String] = js.undefined
  val horizontal: js.UndefOr[String] = js.undefined
}
object MuiSnackBarAnchorOrigin {
  val left = "left"
  val right = "right"
  val center = "center"
  val top = "top"
  val bottom = "bottom"
}


/** JSON fop [[MuiSnackBarProps.transitionDuration]]. */
trait MuiSnackBarTransitionDuration extends js.Object {
  val enter: js.UndefOr[Double] = js.undefined
  val exit: js.UndefOr[Double] = js.undefined
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

