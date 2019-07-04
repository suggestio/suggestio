package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js
import scala.scalajs.js.`|`


object MuiTableCell {

  val component = JsComponent[MuiTableCellProps, Children.Varargs, Null](Mui.TableCell)

  def apply(props: MuiTableCellProps = MuiTableCellProps.empty)(children: VdomNode*) = {
    component(props)(children: _*)
  }

}


trait MuiTableCellProps extends MuiPropsBase {
  val classes: js.UndefOr[MuiTableCellClasses] = js.undefined
  val component: js.UndefOr[String | js.Function | js.Object] = js.undefined
  val padding: js.UndefOr[String] = js.undefined
  val size: js.UndefOr[String] = js.undefined
  val scope: js.UndefOr[String] = js.undefined
  val sortDirection: js.UndefOr[MuiTableCellSort.Sort_t] = js.undefined
  val variant: js.UndefOr[String] = js.undefined
  /** @see [[MuiTypoGraphyAligns]]. */
  val align: js.UndefOr[String] = js.undefined
}
object MuiTableCellProps extends MuiPropsBaseStatic[MuiTableCellProps]


trait MuiTableCellClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val head: js.UndefOr[String] = js.undefined
  val body: js.UndefOr[String] = js.undefined
  val footer: js.UndefOr[String] = js.undefined
  val numeric: js.UndefOr[String] = js.undefined
  val paddingDense: js.UndefOr[String] = js.undefined
  val paddingCheckbox: js.UndefOr[String] = js.undefined
  val paddingNone: js.UndefOr[String] = js.undefined
}


object MuiTableCellVariants {
  val head = "head"
  val body = "body"
  val footer = "footer"
}


object MuiTableCellSort {
  type Sort_t = String | Boolean
  def asc: Sort_t = "asc"
  def desc: Sort_t = "desc"
  def none: Sort_t = false
}
