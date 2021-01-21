package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.02.2020 17:19
  * Description: [[https://material-ui.com/api/expansion-panel-actions/]]
  */
object MuiAccordionActions {

  val component = JsComponent[MuiAccordionActionsProps, Children.Varargs, Null]( Mui.AccordionActions )

  final def apply( props: MuiAccordionActionsProps = MuiPropsBaseStatic.empty )(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiAccordionActionsProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiAccordionActionsClasses]
{
  val disableSpacing: js.UndefOr[Boolean] = js.undefined
}


trait MuiAccordionActionsClasses extends MuiClassesBase {
  val spacing: js.UndefOr[String] = js.undefined
}
