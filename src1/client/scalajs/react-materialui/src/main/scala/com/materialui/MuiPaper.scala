package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js


object MuiPaper {

  val component = JsComponent[MuiPaperProps, Children.Varargs, Null](Mui.Paper)

  final def apply(props: MuiPaperProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiPaperPropsBase
  extends MuiPropsBase
  with MuiPropsBaseComponent
{
  val elevation: js.UndefOr[Int] = js.undefined
  val square: js.UndefOr[Boolean] = js.undefined
}


trait MuiPaperProps
  extends MuiPaperPropsBase
  with MuiPropsBaseClasses[MuiPaperClasses]


trait MuiPaperClasses extends MuiClassesBase {
  val rounded: js.UndefOr[String] = js.undefined
  val elevation0: js.UndefOr[String] = js.undefined
  val elevation1: js.UndefOr[String] = js.undefined
  val elevation2: js.UndefOr[String] = js.undefined
  val elevation3: js.UndefOr[String] = js.undefined
  val elevation4: js.UndefOr[String] = js.undefined
  val elevation5: js.UndefOr[String] = js.undefined
  val elevation6: js.UndefOr[String] = js.undefined
  val elevation7: js.UndefOr[String] = js.undefined
  val elevation8: js.UndefOr[String] = js.undefined
  val elevation9: js.UndefOr[String] = js.undefined
  val elevation10: js.UndefOr[String] = js.undefined
  val elevation11: js.UndefOr[String] = js.undefined
  val elevation12: js.UndefOr[String] = js.undefined
  val elevation13: js.UndefOr[String] = js.undefined
  val elevation14: js.UndefOr[String] = js.undefined
  val elevation15: js.UndefOr[String] = js.undefined
  val elevation16: js.UndefOr[String] = js.undefined
  val elevation17: js.UndefOr[String] = js.undefined
  val elevation18: js.UndefOr[String] = js.undefined
  val elevation19: js.UndefOr[String] = js.undefined
  val elevation20: js.UndefOr[String] = js.undefined
  val elevation21: js.UndefOr[String] = js.undefined
  val elevation22: js.UndefOr[String] = js.undefined
  val elevation23: js.UndefOr[String] = js.undefined
  val elevation24: js.UndefOr[String] = js.undefined
}
