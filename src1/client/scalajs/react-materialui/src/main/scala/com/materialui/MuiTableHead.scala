package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js


object MuiTableHead {

  val component = JsComponent[MuiTableHeadProps, Children.Varargs, Null](Mui.TableHead)

  /**
    * @param children Children passed to table header.
   */
  def apply(props: MuiTableHeadProps = MuiTableHeadProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiTableHeadProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiTableHeadClasses]
  with MuiPropsBaseComponent
object MuiTableHeadProps extends MuiPropsBaseStatic[MuiTableHeadProps]


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
