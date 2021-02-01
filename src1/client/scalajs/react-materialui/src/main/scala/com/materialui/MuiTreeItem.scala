package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.2020 11:25
  * Description: TreeItem API.
  * @see [[https://material-ui.com/ru/api/tree-item/]]
  */
object MuiTreeItem {

  val component = JsForwardRefComponent[MuiTreeItemProps, Children.Varargs, dom.html.Element]( Mui.TreeItem )

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
  val TransitionComponent: js.UndefOr[raw.React.ElementType] = js.undefined
  val TransitionProps: js.UndefOr[js.Object] = js.undefined
  val ContentComponent: js.UndefOr[raw.React.ComponentType[MuiTreeItemContentProps]] = js.undefined
  val ContentProps: js.UndefOr[MuiTreeItemContentProps] = js.undefined
}


trait MuiTreeItemClasses extends MuiClassesBase {
  val expanded,
      selected,
      group,
      content,
      focused,
      disabled,
      iconContainer,
      label: js.UndefOr[String] = js.undefined
}
