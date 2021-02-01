package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js


object MuiTableSortLabel {

  val component = JsForwardRefComponent[MuiTableSortLabelProps, Children.Varargs, dom.html.Element](Mui.TableSortLabel)

  def apply(props: MuiTableSortLabelProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
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


trait MuiTableSortLabelClasses
  extends MuiClassesBase
{
  val active: js.UndefOr[String] = js.undefined
  val icon: js.UndefOr[String] = js.undefined
  val iconDirectionDesc: js.UndefOr[String] = js.undefined
  val iconDirectionAsc: js.UndefOr[String] = js.undefined
}