package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js


object MuiToolBar {

  val component = JsComponent[MuiToolBarProps, Children.Varargs, Null](Mui.Toolbar)

  def apply(props: MuiToolBarProps = MuiToolBarProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON-props для [[MuiToolBar]]. */
trait MuiToolBarProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiToolBarClasses]
{
  val disableGutters: js.UndefOr[Boolean] = js.undefined
  val variant: js.UndefOr[String] = js.undefined
}
object MuiToolBarProps extends MuiPropsBaseStatic[MuiToolBarProps]


trait MuiToolBarClasses extends MuiClassesBase {
  val gutters: js.UndefOr[String] = js.undefined
  val regular: js.UndefOr[String] = js.undefined
  val dense: js.UndefOr[String] = js.undefined
}


/** JSON-модель классов для [[MuiToolBarProps.classes]]. */
object MuiToolBarVariants {
  val dense = "dense"
  val regular = "regular"
}
