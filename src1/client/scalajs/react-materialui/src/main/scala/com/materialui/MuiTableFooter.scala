package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js


object MuiTableFooter {

  val component = JsComponent[js.Object, Children.Varargs, Null](Mui.TableFooter)

  def apply(props: MuiTableFooterProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiTableFooterProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiTableFooterClasses]
  with MuiPropsBaseComponent


trait MuiTableFooterClasses
  extends MuiClassesBase
