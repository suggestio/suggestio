package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.`|`


object MuiDialog {

  val component = JsForwardRefComponent[MuiDialogProps, Children.Varargs, dom.html.Element](Mui.Dialog)

  final def apply(props: MuiDialogProps)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON props for [[MuiDialog]]. */
trait MuiDialogProps
  extends MuiModalPropsBase
  with MuiPropsBaseClasses[MuiDialogClasses]
{
  val fullScreen: js.UndefOr[Boolean] = js.undefined
  val fullWidth: js.UndefOr[Boolean] = js.undefined
  val maxWidth: js.UndefOr[String | Boolean] = js.undefined
  val PaperProps: js.UndefOr[js.Object] = js.undefined
  val scroll: js.UndefOr[String] = js.undefined
  val TransitionComponent: js.UndefOr[Component_t] = js.undefined
  val transitionDuration: js.UndefOr[Double | MuiTransitionDuration] = js.undefined
  val TransitionProps: js.UndefOr[js.Object] = js.undefined
  val open: Boolean
}


/** JSON CSS Classes for [[MuiDialogProps.classes]]. */
trait MuiDialogClasses extends MuiClassesBase {
  val scrollPaper: js.UndefOr[String] = js.undefined
  val scrollBody: js.UndefOr[String] = js.undefined
  val paper: js.UndefOr[String] = js.undefined
  val paperScrollPaper: js.UndefOr[String] = js.undefined
  val paperScrollBody: js.UndefOr[String] = js.undefined
  val paperWidthXs: js.UndefOr[String] = js.undefined
  val paperWidthSm: js.UndefOr[String] = js.undefined
  val paperWidthMd: js.UndefOr[String] = js.undefined
  val paperWidthLg: js.UndefOr[String] = js.undefined
  val paperFullWidth: js.UndefOr[String] = js.undefined
  val paperFullScreen: js.UndefOr[String] = js.undefined
}


/** Possible values (except false) for [[MuiDialogProps.maxWidth]]. */
object MuiDialogMaxWidths {
  val xs = "xs"
  val sm = "sm"
  val md = "md"
  val lg = "lg"
}


/** Values enum for [[MuiDialogProps.scroll]]. */
object MuiDialogScrolls {
  val body = "body"
  val paper = "paper"
}