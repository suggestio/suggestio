package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import japgolly.scalajs.react.vdom._

import scala.scalajs.js
import scala.scalajs.js.|


object MuiTablePagination {

  val component = JsComponent[MuiTablePaginationProps, Children.Varargs, Null](Mui.TablePagination)

  def apply(props: MuiTablePaginationProps)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON props for [[MuiTablePagination.component]]. */
trait MuiTablePaginationProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiTablePaginationClasses]
  with MuiPropsBaseComponent
{
  val ActionsComponent: js.UndefOr[String | js.Function | js.Object] = js.undefined
  val backIconButtonProps: js.UndefOr[js.Object] = js.undefined
  val count: Int
  val labelDisplayedRows: js.UndefOr[js.Function1[FromToCount, React.Node]] = js.undefined
  val labelRowsPerPage: js.UndefOr[js.Function1[FromToCountPage, React.Node]] = js.undefined
  val nextIconButtonProps: js.UndefOr[js.Object] = js.undefined
  val onChangePage: js.UndefOr[js.Function2[ReactEvent, Int, _]] = js.undefined
  val onChangeRowsPerPage: js.UndefOr[js.Function1[ReactEvent, _]] = js.undefined
  val page: Int
  val rowsPerPage: Int
  val rowsPerPageOptions: js.UndefOr[js.Array[Int]] = js.undefined
  val SelectProps: js.UndefOr[js.Object] = js.undefined
}


trait MuiTablePaginationClasses
  extends MuiClassesBase
{
  val toolbar: js.UndefOr[String] = js.undefined
  val spacer: js.UndefOr[String] = js.undefined
  val caption: js.UndefOr[String] = js.undefined
  val selectRoot: js.UndefOr[String] = js.undefined
  val select: js.UndefOr[String] = js.undefined
  val selectIcon: js.UndefOr[String] = js.undefined
  val input: js.UndefOr[String] = js.undefined
  val menuItem: js.UndefOr[String] = js.undefined
  val actions: js.UndefOr[String] = js.undefined
}


@js.native
trait FromToCount extends js.Object {
  val from: Int
  val to: Int
  val count: Int
}

@js.native
trait FromToCountPage extends FromToCount {
  val page: Int
}
