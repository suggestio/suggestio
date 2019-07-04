package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js


object MuiTableBody {

  val component = JsComponent[MuiTableBodyProps, Children.Varargs, Null](Mui.TableBody)

  def apply(props: MuiTableBodyProps = MuiTableBodyProps.empty)(children: VdomNode*) = {
    component(props)(children: _*)
  }

}


trait MuiTableBodyProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiTableBodyClasses]
  with MuiPropsBaseComponent
object MuiTableBodyProps extends MuiPropsBaseStatic[MuiTableBodyProps]


trait MuiTableBodyClasses
  extends MuiClassesBase


@js.native
trait MuiTableBodyM extends js.Object {
  def createRowCheckboxColumn(): Unit = js.native

  def createRows(): Unit = js.native

  def flattenRanges(): Unit = js.native

  def genRangeOfValues(): Unit = js.native

  def getColumnId(): Unit = js.native

  def getSelectedRows(): Unit = js.native

  def isRowSelected(): Unit = js.native

  def isValueInRange(): Unit = js.native

  def processRowSelection(): Unit = js.native

  def splitRange(): Unit = js.native
}
