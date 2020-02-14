package com.materialui

import japgolly.scalajs.react._
import scala.scalajs.js


object MuiDivider {

  val component = JsComponent[MuiDividerProps, Children.None, Null](Mui.Divider)

  final def apply(props: MuiDividerProps = MuiPropsBaseStatic.empty) =
    component(props)

}
        

trait MuiDividerProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiDividerClasses]
  with MuiPropsBaseComponent
{
  val absolute: js.UndefOr[Boolean] = js.undefined
  val light: js.UndefOr[Boolean] = js.undefined
  val variant: js.UndefOr[String] = js.undefined
}


object MuiDividerVariants {
  val inset = "inset"
  val fullWidth = "fullWidth"
  val middle = "middle"
}

trait MuiDividerClasses extends MuiClassesBase {
  val absolute: js.UndefOr[String] = js.undefined
  val inset: js.UndefOr[String] = js.undefined
  val light: js.UndefOr[String] = js.undefined
  val middle: js.UndefOr[String] = js.undefined
}
