package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js


object MuiTableFooter {

  val component = JsForwardRefComponent[js.Object, Children.Varargs, dom.html.Element](Mui.TableFooter)

  def apply(props: MuiTableFooterProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiTableFooterProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiTableFooterClasses]
  with MuiPropsBaseComponent


trait MuiTableFooterClasses
  extends MuiClassesBase
