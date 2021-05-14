package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js


object MuiIconButton {

  val component = JsForwardRefComponent[MuiIconButtonProps, Children.Varargs, dom.html.Element](Mui.IconButton)

  final def apply(props: MuiIconButtonProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)


  type Edge <: js.Any
  object Edge {
    final def START = "start".asInstanceOf[Edge]
    final def END = "end".asInstanceOf[Edge]
    final def FALSE = false.asInstanceOf[Edge]
  }

}


/** Props for [[MuiIconButton]]. */
trait MuiIconButtonPropsBase extends MuiButtonBaseCommonProps {
  val edge: js.UndefOr[MuiIconButton.Edge] = js.undefined
  val color, size: js.UndefOr[String] = js.undefined
  val disableFocusRipple: js.UndefOr[Boolean] = js.undefined
}
trait MuiIconButtonProps
  extends MuiIconButtonPropsBase
  with MuiPropsBaseClasses[MuiIconButtonClasses]


/** CSS Classes for [[MuiIconButtonProps.classes]]. */
trait MuiIconButtonClasses extends MuiClassesBase {
  val colorInherit: js.UndefOr[String] = js.undefined
  val colorPrimary: js.UndefOr[String] = js.undefined
  val colorSecondary: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val label: js.UndefOr[String] = js.undefined
}
