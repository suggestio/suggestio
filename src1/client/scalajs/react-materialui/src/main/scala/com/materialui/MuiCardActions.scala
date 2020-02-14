package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js


object MuiCardActions {

  val component = JsComponent[MuiCardActionsProps, Children.Varargs, Null](Mui.CardActions)

  final def apply(props: MuiCardActionsProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiCardActionsProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiCardActionsClasses]
{
  val disableActionSpacing: js.UndefOr[Boolean] = js.undefined
}


trait MuiCardActionsClasses extends MuiClassesBase {
  val action: js.UndefOr[String] = js.undefined
}
