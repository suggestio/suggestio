package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js


object MuiIconButton {

  val component = JsComponent[MuiIconButtonProps, Children.Varargs, Null](Mui.IconButton)

  final def apply(props: MuiIconButtonProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON для [[MuiIconButton]] props. */
trait MuiIconButtonProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiIconButtonClasses]
{
  val color: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[Boolean] = js.undefined
  val disableRipple: js.UndefOr[Boolean] = js.undefined
}


/** JSON для [[MuiIconButtonProps.classes]]. */
trait MuiIconButtonClasses extends MuiClassesBase {
  val colorInherit: js.UndefOr[String] = js.undefined
  val colorPrimary: js.UndefOr[String] = js.undefined
  val colorSecondary: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val label: js.UndefOr[String] = js.undefined
}


@js.native
trait MuiIconButtonM extends js.Object {
  def hideTooltip(): Unit = js.native

  def setKeyboardFocus(): Unit = js.native

  def showTooltip(): Unit = js.native
}
