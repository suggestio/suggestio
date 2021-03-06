package com.materialui

import japgolly.scalajs.react._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLInputElement

import scala.scalajs.js
import scala.scalajs.js.`|`
import scala.scalajs.js.annotation.JSName


object MuiSwitch {

  def mkComponent = JsForwardRefComponent[MuiSwitchProps, Children.None, dom.html.Element](_: js.Any)

  lazy val component = mkComponent( Mui.Switch )

  def apply(props: MuiSwitchProps = MuiPropsBaseStatic.empty) =
    component(props)

}


trait MuiSwitchProps
  extends MuiIconButtonPropsBase
  with MuiPropsBaseClasses[MuiSwitchClasses]
{
  val checked: js.UndefOr[Boolean | String] = js.undefined
  val checkedIcon: js.UndefOr[raw.React.Node] = js.undefined
  val icon: js.UndefOr[raw.React.Node] = js.undefined
  val id: js.UndefOr[String] = js.undefined
  val inputProps: js.UndefOr[js.Object] = js.undefined
  val inputRef: js.UndefOr[js.Function1[HTMLInputElement, _] | js.Object] = js.undefined
  @JSName("onChange")
  val onChange2: js.UndefOr[js.Function2[ReactEventFromInput, Boolean, _]] = js.undefined
  val value: js.UndefOr[String | Double | Boolean] = js.undefined
}


trait MuiSwitchClasses extends MuiClassesBase {
  val icon: js.UndefOr[String] = js.undefined
  val iconChecked: js.UndefOr[String] = js.undefined
  val switchBase: js.UndefOr[String] = js.undefined
  val checked: js.UndefOr[String] = js.undefined
  val colorPrimary: js.UndefOr[String] = js.undefined
  val colorSecondary: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val bar: js.UndefOr[String] = js.undefined
}
