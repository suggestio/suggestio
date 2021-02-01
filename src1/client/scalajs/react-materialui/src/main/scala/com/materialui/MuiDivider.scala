package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode
import org.scalajs.dom

import scala.scalajs.js


object MuiDivider {

  val component = JsForwardRefComponent[MuiDividerProps, Children.Varargs, dom.html.Element](Mui.Divider)

  final def apply(props: MuiDividerProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

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
