package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js


object MuiTableSortLabel {

  val component = JsComponent[MuiTableSortLabelProps, Children.Varargs, Null](Mui.TableSortLabel)

  def apply(props: MuiTableSortLabelProps = MuiTableSortLabelProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiTableSortLabelProps
  extends MuiButtonBaseCommonProps
  with MuiPropsBaseClasses[MuiTableSortLabelClasses]
{
  val active: js.UndefOr[Boolean] = js.undefined
  val direction: js.UndefOr[String] = js.undefined
  val hideSortIcon: js.UndefOr[Boolean] = js.undefined
  val IconComponent: js.UndefOr[js.Function] = js.undefined
}
object MuiTableSortLabelProps extends MuiPropsBaseStatic[MuiTableSortLabelProps]


trait MuiTableSortLabelClasses
  extends MuiClassesBase
{
  val active: js.UndefOr[String] = js.undefined
  val icon: js.UndefOr[String] = js.undefined
  val iconDirectionDesc: js.UndefOr[String] = js.undefined
  val iconDirectionAsc: js.UndefOr[String] = js.undefined
}