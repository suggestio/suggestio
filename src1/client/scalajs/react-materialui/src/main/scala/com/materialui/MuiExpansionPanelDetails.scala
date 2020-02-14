package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.02.2020 17:27
  * Description: [[https://material-ui.com/api/expansion-panel-details/]]
  */
object MuiExpansionPanelDetails {

  val component = JsComponent[MuiExpansionPanelDetailsProps, Children.Varargs, Null]( Mui.ExpansionPanelDetails )

  final def apply(props: MuiExpansionPanelDetailsProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiExpansionPanelDetailsProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiExpansionPanelDetailsClasses]


trait MuiExpansionPanelDetailsClasses extends MuiClassesBase
