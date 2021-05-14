package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js


object MuiButton {

  val component = JsForwardRefComponent[MuiButtonProps, Children.Varargs, dom.html.Element](Mui.Button)

  final def apply(props: MuiButtonProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** Props for [[MuiButton]]. */
trait MuiButtonProps
  extends MuiButtonBaseCommonProps
  with MuiPropsBaseClasses[MuiButtonClasses]
{
  val color: js.UndefOr[String] = js.undefined
  val disableFocusRipple: js.UndefOr[Boolean] = js.undefined
  val fullWidth: js.UndefOr[Boolean] = js.undefined
  val href: js.UndefOr[String] = js.undefined
  val mini: js.UndefOr[Boolean] = js.undefined
  val size: js.UndefOr[String] = js.undefined
  val variant: js.UndefOr[String] = js.undefined
  val startIcon: js.UndefOr[raw.React.Node] = js.undefined
}


/** JSON clasess for [[MuiButtonProps]]. */
trait MuiButtonClasses extends MuiClassesBase {
  // TODO https://material-ui.com/api/button/#css-api
}


object MuiButtonSizes {
  val small = "small"
  val medium = "medium"
  val large = "large"
}

object MuiButtonVariants {
  val text = "text"
  val outlined = "outlined"
  val contained = "contained"
}
