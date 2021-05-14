package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.|


object MuiList {

  val component = JsForwardRefComponent[MuiListProps, Children.Varargs, dom.html.Element](Mui.List)

  /** @param children These are usually `ListItem`s that are passed to be part of the list. */
  final def apply(props: MuiListProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** Props for [[MuiList]]. */
trait MuiListProps extends MuiPropsBase {
  val classes:        js.UndefOr[MuiListClasses] = js.undefined
  val component:      js.UndefOr[String | js.Object] = js.undefined
  val dense:          js.UndefOr[Boolean] = js.undefined
  val disablePadding: js.UndefOr[Boolean] = js.undefined
  val subheader:      js.UndefOr[React.Node] = js.undefined
}


/** CSS Classes for [[MuiListProps.classes]]. */
trait MuiListClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val padding: js.UndefOr[String] = js.undefined
  val dense: js.UndefOr[String] = js.undefined
  val subheader: js.UndefOr[String] = js.undefined
}
