package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js


case object MuiCard {

  val component = JsForwardRefComponent[MuiCardProps, Children.Varargs, dom.html.Element](Mui.Card)

  final def apply(props: MuiCardProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON props for [[MuiCard]]. */
trait MuiCardProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiCardClasses]
{
  val raised: js.UndefOr[Boolean] = js.undefined
}


/** JSON model for [[MuiCardProps.classes]]. */
trait MuiCardClasses extends MuiClassesBase
