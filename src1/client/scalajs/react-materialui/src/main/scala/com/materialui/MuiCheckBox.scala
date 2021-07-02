package com.materialui

import japgolly.scalajs.react._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLInputElement

import scala.scalajs.js
import scala.scalajs.js.`|`


object MuiCheckBox {

  val component = JsForwardRefComponent[MuiCheckBoxProps, Children.None, dom.html.Element](Mui.CheckBox)

  final def apply(props: MuiCheckBoxProps = MuiPropsBaseStatic.empty) =
    component(props)

}


trait MuiCheckBoxProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiCheckBoxClasses]
{
  val checked: js.UndefOr[Boolean | String] = js.undefined
  val checkedIcon: js.UndefOr[raw.React.Node] = js.undefined
  val color: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[Boolean] = js.undefined
  val disableRipple: js.UndefOr[Boolean] = js.undefined
  val icon: js.UndefOr[raw.React.Node] = js.undefined
  val id: js.UndefOr[String] = js.undefined
  val indeterminate: js.UndefOr[Boolean] = js.undefined
  val indeterminateIcon: js.UndefOr[raw.React.Node] = js.undefined
  val inputProps: js.UndefOr[js.Object] = js.undefined
  val inputRef: js.UndefOr[js.Function1[HTMLInputElement, _] | js.Object] = js.undefined
  // onChange2 removed: working unstable, $2 == js.undefined, not boolean.
  val `type`: js.UndefOr[String] = js.undefined
  val value: js.UndefOr[String] = js.undefined
}


trait MuiCheckBoxClasses extends MuiClassesBase {
  val checked: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val indeterminate: js.UndefOr[String] = js.undefined
  val colorPrimary: js.UndefOr[String] = js.undefined
  val colorSecondary: js.UndefOr[String] = js.undefined
}
