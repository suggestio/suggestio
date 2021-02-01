package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom


object MuiCardContent {

  val component = JsForwardRefComponent[MuiCardContentProps, Children.Varargs, dom.html.Element](Mui.CardContent)

  final def apply(props: MuiCardContentProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiCardContentProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiCardContentClasses]
  with MuiPropsBaseComponent


trait MuiCardContentClasses extends MuiClassesBase
