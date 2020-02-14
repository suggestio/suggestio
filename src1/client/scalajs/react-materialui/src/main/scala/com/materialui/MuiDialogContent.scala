package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.10.18 22:29
  * Description: Dialog content APIs.
  */
object MuiDialogContent {

  val component = JsComponent[MuiDialogContentProps, Children.Varargs, Null](Mui.DialogContent)

  final def apply(props: MuiDialogContentProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON props for [[MuiDialogContent.component]]. */
trait MuiDialogContentProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiDialogContentClasses]


/** Props for [[MuiDialogContentProps.classes]]. */
trait MuiDialogContentClasses
  extends MuiClassesBase
