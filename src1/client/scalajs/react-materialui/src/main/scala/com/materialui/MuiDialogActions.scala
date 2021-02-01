package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.10.18 22:29
  * Description: Dialog content APIs.
  */
object MuiDialogActions {

  val component = JsComponent[MuiDialogActionsProps, Children.Varargs, dom.html.Element](Mui.DialogActions)

  final def apply(props: MuiDialogActionsProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON props for [[MuiDialogActions.component]]. */
trait MuiDialogActionsProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiDialogActionsClasses]
{
  val disableSpacing: js.UndefOr[Boolean] = js.undefined
}


/** Props for [[MuiDialogActionsProps.classes]]. */
trait MuiDialogActionsClasses
  extends MuiClassesBase
{
  val spacing: js.UndefOr[Boolean] = js.undefined
}
