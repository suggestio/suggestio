package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.02.2020 22:32
  *
  * @see [[https://material-ui.com/components/selects/]]
  */
object MuiSelect {

  val component = JsForwardRefComponent[MuiSelectProps, Children.Varargs, dom.html.Element]( Mui.Select )

  final def apply(props: MuiSelectProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiSelectProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiSelectClasses]
{
  val autoWidth: js.UndefOr[Boolean] = js.undefined
  val defaultValue: js.UndefOr[js.Any] = js.undefined
  val displayEmpty: js.UndefOr[Boolean] = js.undefined
  val IconComponent: js.UndefOr[js.Any] = js.undefined
  val input: js.UndefOr[raw.React.Element] = js.undefined
  val inputProps: js.UndefOr[js.Object] = js.undefined
  val label: js.UndefOr[raw.React.Node] = js.undefined
  val labelId: js.UndefOr[String] = js.undefined
  val MenuProps: js.UndefOr[MuiMenuPropsBase] = js.undefined
  val multiple: js.UndefOr[Boolean] = js.undefined
  val native: js.UndefOr[Boolean] = js.undefined
  /** function(event: object, child?: object) => void
    * child: The react element that was selected when native is false (default).
    */
  @JSName("onChange")
  val onChange2: js.UndefOr[js.Function2[ReactEvent, js.UndefOr[raw.React.Element], Unit]] = js.undefined
  val onClose: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val onOpen: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val open: js.UndefOr[Boolean] = js.undefined
  val renderValue: js.UndefOr[js.Function1[js.Any, raw.React.Node]] = js.undefined
  val SelectDisplayProps: js.UndefOr[js.Object] = js.undefined
  val value: js.UndefOr[js.Any] = js.undefined
  val variant: js.UndefOr[MuiTextField.Variant] = js.undefined
}


trait MuiSelectClasses extends MuiClassesBase {
  val select: js.UndefOr[String] = js.undefined
  val filled: js.UndefOr[String] = js.undefined
  val outlined: js.UndefOr[String] = js.undefined
  val selectMenu: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val icon: js.UndefOr[String] = js.undefined
  val iconOpen: js.UndefOr[String] = js.undefined
  val iconFilled: js.UndefOr[String] = js.undefined
  val iconOutlined: js.UndefOr[String] = js.undefined
}
