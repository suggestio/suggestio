package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js


object MuiCardActionArea {

  val component = JsForwardRefComponent[MuiCardActionAreaProps, Children.Varargs, dom.html.Element]( Mui.CardActionArea )

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
