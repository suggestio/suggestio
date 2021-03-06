package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.10.18 22:29
  * Description: Dialog title APIs.
  */
object MuiDialogTitle {

  val component = JsForwardRefComponent[MuiDialogTitleProps, Children.Varargs, dom.html.Element](Mui.DialogTitle)

  final def apply(props: MuiDialogTitleProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON props for [[MuiDialogTitle.component]]. */
trait MuiDialogTitleProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiDialogTitleClasses]


/** Props for [[MuiDialogTitleProps.classes]]. */
trait MuiDialogTitleClasses
  extends MuiClassesBase
