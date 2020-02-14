package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js


object MuiCardActionArea {

  val component = JsComponent[MuiCardActionAreaProps, Children.Varargs, Null]( Mui.CardActionArea )

  final def apply(props: MuiCardActionAreaProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiCardActionAreaProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiCardActionAreaClasses]


trait MuiCardActionAreaClasses extends MuiClassesBase {
  val focusVisible: js.UndefOr[String] = js.undefined
  val focusHighlight: js.UndefOr[String] = js.undefined
}
