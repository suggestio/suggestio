package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js


object MuiTableHead {

  val component = JsForwardRefComponent[MuiTableHeadProps, Children.Varargs, dom.html.Element](Mui.TableHead)

  /**
    * @param children Children passed to table header.
   */
  def apply(props: MuiTableHeadProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiTableHeadProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiTableHeadClasses]
  with MuiPropsBaseComponent


trait MuiTableHeadClasses
  extends MuiClassesBase


@js.native
trait MuiTableHeaderM extends js.Object {
  def createBaseHeaderRow(): Unit = js.native

  def createSuperHeaderRow(): Unit = js.native

  def createSuperHeaderRows(): Unit = js.native

  def getCheckboxPlaceholder(): Unit = js.native

  def getSelectAllCheckboxColumn(): Unit = js.native
}
