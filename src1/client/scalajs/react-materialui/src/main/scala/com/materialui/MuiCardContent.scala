package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._


object MuiCardContent {

  val component = JsComponent[MuiCardContentProps, Children.Varargs, Null](Mui.CardContent)

  final def apply(props: MuiCardContentProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiCardContentProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiCardContentClasses]
  with MuiPropsBaseComponent


trait MuiCardContentClasses extends MuiClassesBase
