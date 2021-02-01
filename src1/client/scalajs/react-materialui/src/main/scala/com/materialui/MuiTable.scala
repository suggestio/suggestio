package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js


object MuiTable {

  val component = JsForwardRefComponent[MuiTableProps, Children.Varargs, dom.html.Element](Mui.Table)

  def apply(props: MuiTableProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON props for [[MuiTable.component]]. */
trait MuiTableProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiTableClasses]
  with MuiPropsBaseComponent
{
  val padding: js.UndefOr[String] = js.undefined
}


trait MuiTableClasses
  extends MuiClassesBase


@js.native
trait MuiTableM extends js.Object {
  def createTableBody(): Unit = js.native

  def createTableFooter(): Unit = js.native

  def createTableHeader(): Unit = js.native

  def isScrollbarVisible(): Boolean = js.native
}


object MuiTablePaddings {
  val default = "default"
  val checkbox = "checkbox"
  val dense = "dense"
  val none = "none"
}
