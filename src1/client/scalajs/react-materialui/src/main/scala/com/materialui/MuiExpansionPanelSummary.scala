package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.02.2020 17:32
  * Description: [[https://material-ui.com/api/expansion-panel-summary/]]
  */
object MuiExpansionPanelSummary {

  val component = JsComponent[MuiExpansionPanelSummaryProps, Children.Varargs, Null]( Mui.ExpansionPanelSummary )

  final def apply( props: MuiExpansionPanelSummaryProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component( props )( children: _* )

}


trait MuiExpansionPanelSummaryProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiExpansionPanelSummaryClasses]
{
  val expandIcon: js.UndefOr[raw.React.Node] = js.undefined
  val IconButtonProps: js.UndefOr[MuiIconButtonProps] = js.undefined
}


trait MuiExpansionPanelSummaryClasses extends MuiClassesBase {
  val expanded: js.UndefOr[String] = js.undefined
  val focused: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val content: js.UndefOr[String] = js.undefined
  val expandIcon: js.UndefOr[String] = js.undefined
}
