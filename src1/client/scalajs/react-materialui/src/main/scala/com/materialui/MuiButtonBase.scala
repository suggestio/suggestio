package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.|


object MuiButtonBase {

  val component = JsForwardRefComponent[MuiButtonBaseProps, Children.Varargs, dom.html.Element](Mui.Button)

  final def apply(props: MuiButtonBaseProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}

// Пошаренные пропертисы:
trait MuiButtonBaseCommonProps
  extends MuiPropsBase
  with MuiPropsBaseComponent
{
  val action: js.UndefOr[js.Function1[js.Object, _]] = js.undefined
  val buttonRef: js.UndefOr[js.Function | js.Object] = js.undefined
  val centerRipple: js.UndefOr[Boolean] = js.undefined
  val disabled: js.UndefOr[Boolean] = js.undefined
  val disableRipple: js.UndefOr[Boolean] = js.undefined
  val disableTouchRipple: js.UndefOr[Boolean] = js.undefined
  val focusRipple: js.UndefOr[Boolean] = js.undefined
  val focusVisibleClassName: js.UndefOr[String] = js.undefined
  val onFocusVisible: js.UndefOr[js.Function] = js.undefined
  val TouchRippleProps: js.UndefOr[js.Object] = js.undefined
  val `type`: js.UndefOr[String] = js.undefined
}


/** Модель props для [[MuiButtonBase]]. */
trait MuiButtonBaseProps
  extends MuiPropsBase
  with MuiButtonBaseCommonProps
  with MuiPropsBaseClasses[MuiButtonBaseClasses]

/** JSON для [[MuiButtonBaseProps]].classes. */
trait MuiButtonBaseClasses extends MuiClassesBase {
  val disabled: js.UndefOr[String] = js.undefined
  val focusVisible: js.UndefOr[String] = js.undefined
}


object MuiButtonBaseSizes {
  val small = "small"
  val medium = "medium"
  val large = "large"
}

object MuiButtonBaseVariants {
  val text = "text"
  val flat = "flat"
  val outlined = "outlined"
  val contained = "contained"
  val raised = "raised"
  val fab = "fab"
  val extendedFab = "extendedFab"
}
