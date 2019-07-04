package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js


object MuiAppBar {

  val component = JsComponent[MuiAppBarProps, Children.Varargs, Null](Mui.AppBar)

  def apply(props: MuiAppBarProps = MuiAppBarProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiAppBarProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiAppBarClasses]
{
  val color: js.UndefOr[String] = js.undefined
  val position: js.UndefOr[String] = js.undefined
}
object MuiAppBarProps extends MuiPropsBaseStatic[MuiAppBarProps]


trait MuiAppBarClasses extends MuiClassesBase {
  val positionFixed: js.UndefOr[String] = js.undefined
  val positionAbsolute: js.UndefOr[String] = js.undefined
  val positionSticky: js.UndefOr[String] = js.undefined
  val positionStatic: js.UndefOr[String] = js.undefined
  val positionRelative: js.UndefOr[String] = js.undefined
  val colorDefault: js.UndefOr[String] = js.undefined
  val colorPrimary: js.UndefOr[String] = js.undefined
  val colorSecondary: js.UndefOr[String] = js.undefined
}

