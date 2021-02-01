package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.2020 10:55
  * Description: TreeView component API.
  * @see [[https://material-ui.com/ru/components/tree-view/]]
  */
object MuiTreeView {

  val component = JsForwardRefComponent[MuiTreeViewProps, Children.Varargs, dom.html.Element]( Mui.TreeView )

  def apply(props: MuiTreeViewProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)
}


trait MuiTreeViewProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiTreeViewClasses]
{

  val defaultCollapseIcon: js.UndefOr[raw.React.Node] = js.undefined
  val defaultEndIcon: js.UndefOr[raw.React.Node] = js.undefined
  // Uncontrolled
  val defaultExpanded: js.UndefOr[js.Array[String]] = js.undefined
  val defaultExpandIcon: js.UndefOr[raw.React.Node] = js.undefined
  val defaultParentIcon: js.UndefOr[raw.React.Node] = js.undefined
  // Uncontrolled
  val defaultSelected: js.UndefOr[js.Array[String] | String] = js.undefined
  val disableSelection: js.UndefOr[Boolean] = js.undefined

  // Controlled
  val expanded: js.UndefOr[js.Array[String]] = js.undefined

  val multiSelect: js.UndefOr[Boolean] = js.undefined

  /** Callback fired when tree items are selected/unselected.
    *   function(event: object, value: array | string) => void
    *   event: The event source of the callback
    *   value: of the selected nodes. When multiSelect is true this is an array of strings; when false (default) a string.
    */
  val onNodeSelect: js.UndefOr[js.Function2[ReactEvent, js.Array[String] | String, Unit]] = js.undefined

  /** Callback fired when tree items are expanded/collapsed.
    *   function(event: object, nodeIds: array) => void
    *   event: The event source of the callback.
    *   nodeIds: The ids of the expanded nodes.
    */
  val onNodeToggle: js.UndefOr[js.Function2[ReactEvent, js.Array[String], Unit]] = js.undefined

  // Controlled
  val selected: js.UndefOr[js.Array[String] | String] = js.undefined

}


trait MuiTreeViewClasses extends MuiClassesBase
