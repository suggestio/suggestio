package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.10.18 22:29
  * Description: Dialog content text APIs.
  */
object MuiDialogContentText {

  val component = JsForwardRefComponent[MuiDialogContentTextProps, Children.Varargs, dom.html.Element](Mui.DialogContentText)

  final def apply(props: MuiDialogContentTextProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON props for [[MuiDialogContentText.component]]. */
trait MuiDialogContentTextProps
  extends MuiTypoGraphyPropsBase
  with MuiPropsBaseClasses[MuiDialogContentTextClasses]


/** Props for [[MuiDialogContentTextProps.classes]]. */
trait MuiDialogContentTextClasses
  extends MuiClassesBase
