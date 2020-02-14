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
object MuiExpansionPanelActions {

  val component = JsComponent[MuiExpansionPanelActionsProps, Children.Varargs, Null]( Mui.ExpansionPanelActions )

  final def apply( props: MuiExpansionPanelActionsProps = MuiPropsBaseStatic.empty )(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiExpansionPanelActionsProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiExpansionPanelActionsClasses]
{
  val disableSpacing: js.UndefOr[Boolean] = js.undefined
}


trait MuiExpansionPanelActionsClasses extends MuiClassesBase {
  val spacing: js.UndefOr[String] = js.undefined
}
