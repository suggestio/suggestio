package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js


object MuiButton {

  val component = JsComponent[js.Object, Children.Varargs, Null](Mui.FlatButton)

  def apply(props: MuiButtonProps)(children: VdomNode*) =
    component(props)(children: _*)

}


/** Модель props для [[MuiButton]]. */
trait MuiButtonProps extends MuiPropsBase {
  val classes: js.UndefOr[String] = js.undefined
  val color: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[Boolean] = js.undefined
  val disableFocusRipple: js.UndefOr[Boolean] = js.undefined
  val disableRipple: js.UndefOr[Boolean] = js.undefined
  val fullWidth: js.UndefOr[Boolean] = js.undefined
  val href: js.UndefOr[String] = js.undefined
  val mini: js.UndefOr[Boolean] = js.undefined
  val size: js.UndefOr[String] = js.undefined
  val variant: js.UndefOr[String] = js.undefined
}


/** JSON для [[MuiButtonProps]].classes. */
trait MuiButtonClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  // TODO https://material-ui.com/api/button/#css-api
}


object MuiButtonSizes {
  val small = "small"
  val medium = "medium"
  val large = "large"
}

object MuiButtonVariants {
  val text = "text"
  val flat = "flat"
  val outlined = "outlined"
  val contained = "contained"
  val raised = "raised"
  val fab = "fab"
  val extendedFab = "extendedFab"
}
