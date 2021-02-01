package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.02.2020 17:32
  * Description: [[https://material-ui.com/api/expansion-panel-summary/]]
  */
object MuiAccordionSummary {

  val component = JsForwardRefComponent[MuiAccordionSummaryProps, Children.Varargs, dom.html.Element]( Mui.AccordionSummary )

  final def apply( props: MuiAccordionSummaryProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component( props )( children: _* )

}


trait MuiAccordionSummaryProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiAccordionSummaryClasses]
{
  val expandIcon: js.UndefOr[raw.React.Node] = js.undefined
  val IconButtonProps: js.UndefOr[MuiIconButtonProps] = js.undefined
}


trait MuiAccordionSummaryClasses extends MuiClassesBase {
  val expanded: js.UndefOr[String] = js.undefined
  val focused: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val content: js.UndefOr[String] = js.undefined
  val expandIcon: js.UndefOr[String] = js.undefined
}
