package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.02.2020 17:00
  * Description: [[https://material-ui.com/api/expansion-panel/]]
  */
object MuiAccordion {

  val component = JsForwardRefComponent[MuiAccordionProps, Children.Varargs, dom.html.Element]( Mui.Accordion )

  final def apply(props: MuiAccordionProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiAccordionProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiAccordionClasses]
{
  val defaultExpanded: js.UndefOr[Boolean] = js.undefined
  val disabled: js.UndefOr[Boolean] = js.undefined
  val expanded: js.UndefOr[Boolean] = js.undefined
  @JSName("onChange")
  val onChange2: js.UndefOr[js.Function2[ReactEvent, Boolean, Unit]] = js.undefined
  val TransitionComponent: js.UndefOr[Component_t] = js.undefined
  val TransitionProps: js.UndefOr[js.Object] = js.undefined
}


trait MuiAccordionClasses extends MuiClassesBase {
  val rounded: js.UndefOr[String] = js.undefined
  val expanded: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
}
