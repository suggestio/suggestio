package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js


object MuiCardMedia {

  val component = JsForwardRefComponent[MuiCardMediaProps, Children.Varargs, dom.html.Element](Mui.CardMedia)

  final def apply(props: MuiCardMediaProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiCardMediaProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiCardMediaClasses]
  with MuiPropsBaseComponent
{
  val image: js.UndefOr[String] = js.undefined
  val src: js.UndefOr[String] = js.undefined
}


trait MuiCardMediaClasses extends MuiClassesBase {
  val media: js.UndefOr[String] = js.undefined
}
