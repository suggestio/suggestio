package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js


object MuiToolBar {

  val component = JsForwardRefComponent[MuiToolBarProps, Children.Varargs, dom.html.Element](Mui.Toolbar)

  def apply(props: MuiToolBarProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** Props для [[MuiToolBar]]. */
trait MuiToolBarProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiToolBarClasses]
{
  val disableGutters: js.UndefOr[Boolean] = js.undefined
  val variant: js.UndefOr[String] = js.undefined
}


trait MuiToolBarClasses extends MuiClassesBase {
  val gutters: js.UndefOr[String] = js.undefined
  val regular: js.UndefOr[String] = js.undefined
  val dense: js.UndefOr[String] = js.undefined
}


/** CSS Classes for [[MuiToolBarProps.classes]]. */
object MuiToolBarVariants {
  val dense = "dense"
  val regular = "regular"
}
