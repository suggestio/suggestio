package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.`|`


object MuiListItem {

  val component = JsForwardRefComponent[MuiListItemProps, Children.Varargs, dom.html.Element](Mui.ListItem)

  /** @param children Children passed into the `ListItem`. */
  final def apply(props: MuiListItemProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiListItemPropsBase
  extends MuiPropsBase
  with MuiPropsBaseComponent
{
  val button:                      js.UndefOr[Boolean]                                = js.undefined
  val selected:                    js.UndefOr[Boolean]                                = js.undefined
  val divider:                     js.UndefOr[Boolean]                                = js.undefined
  val disabled:                    js.UndefOr[Boolean]                                = js.undefined
  val dense:                       js.UndefOr[Boolean]                                = js.undefined
  val disableGutters:              js.UndefOr[Boolean]                                = js.undefined

  val ContainerComponent:          js.UndefOr[String | React.Element]                 = js.undefined
}

trait MuiListItemProps
  extends MuiListItemPropsBase
  with MuiPropsBaseClasses[MuiListItemClasses]


trait MuiListItemClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val container: js.UndefOr[String] = js.undefined
  val focusVisible: js.UndefOr[String] = js.undefined
  val dense: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val divider: js.UndefOr[String] = js.undefined
  val gutters: js.UndefOr[String] = js.undefined
  val button: js.UndefOr[String] = js.undefined
  val secondaryAction: js.UndefOr[String] = js.undefined
  val selected: js.UndefOr[String] = js.undefined
}
