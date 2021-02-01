package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js


object MuiTableRow {

  val component = JsForwardRefComponent[MuiTableRowProps, Children.Varargs, dom.html.Element](Mui.TableRow)

  def apply(props: MuiTableRowProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiTableRowProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiTableRowClasses]
  with MuiPropsBaseComponent
{
  val hover: js.UndefOr[Boolean] = js.undefined
  val selected: js.UndefOr[Boolean] = js.undefined
}


trait MuiTableRowClasses
  extends MuiTableClasses
{
  val selected: js.UndefOr[String] = js.undefined
  val hover: js.UndefOr[String] = js.undefined
  val head: js.UndefOr[String] = js.undefined
  val footer: js.UndefOr[String] = js.undefined
}


@js.native
trait MuiTableRowM extends js.Object {
  def onRowClick(): Unit = js.native

  def onRowHover(): Unit = js.native

  def onRowHoverExit(): Unit = js.native
}
