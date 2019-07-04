package com.materialui

import japgolly.scalajs.react._
import org.scalajs.dom.raw.HTMLInputElement

import scala.scalajs.js
import scala.scalajs.js.`|`
import scala.scalajs.js.annotation.JSName


object MuiCheckBox {

  val component = JsComponent[MuiCheckBoxProps, Children.None, Null](Mui.CheckBox)

  def apply(props: MuiCheckBoxProps = MuiCheckBoxProps.empty) =
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
  @JSName("onChange")
  val onChange2: js.UndefOr[js.Function2[ReactEventFromInput, Boolean, _]] = js.undefined
  val `type`: js.UndefOr[String] = js.undefined
  val value: js.UndefOr[String] = js.undefined
}
object MuiCheckBoxProps extends MuiPropsBaseStatic[MuiCheckBoxProps]


trait MuiCheckBoxClasses extends MuiClassesBase {
  val checked: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val indeterminate: js.UndefOr[String] = js.undefined
  val colorPrimary: js.UndefOr[String] = js.undefined
  val colorSecondary: js.UndefOr[String] = js.undefined
}
