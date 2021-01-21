package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.02.2020 17:27
  * Description: [[https://material-ui.com/api/expansion-panel-details/]]
  */
object MuiAccordionDetails {

  val component = JsComponent[MuiAccordionDetailsProps, Children.Varargs, Null]( Mui.AccordionDetails )

  final def apply(props: MuiAccordionDetailsProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiAccordionDetailsProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiAccordionDetailsClasses]


trait MuiAccordionDetailsClasses extends MuiClassesBase
