package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.2020 11:25
  * Description: TreeItem API.
  * @see [[https://material-ui.com/ru/api/tree-item/]]
  */
object MuiTreeItem {

  val component = JsComponent[MuiTreeItemProps, Children.Varargs, Null]( Mui.TreeItem )

  def apply(props: MuiTreeItemProps)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiTreeItemProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiTreeItemClasses]
{
  val collapseIcon: js.UndefOr[raw.React.Node] = js.undefined
  val endIcon: js.UndefOr[raw.React.Node] = js.undefined
  val expandIcon: js.UndefOr[raw.React.Node] = js.undefined
  val icon: js.UndefOr[raw.React.Node] = js.undefined
  val label: js.UndefOr[raw.React.Node] = js.undefined
  val nodeId: String
  val onIconClick: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val onLabelClick: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val TransitionComponent: js.UndefOr[raw.React.ElementType] = js.undefined
  val TransitionProps: js.UndefOr[js.Object] = js.undefined
}


trait MuiTreeItemClasses extends MuiClassesBase {
  val expanded,
      selected,
      group,
      content,
      iconContainer,
      label: js.UndefOr[String] = js.undefined
}
