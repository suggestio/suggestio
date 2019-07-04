package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js


case object MuiCard {

  val component = JsComponent[MuiCardProps, Children.Varargs, Null](Mui.Card)

  def apply(props: MuiCardProps = MuiCardProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON props for [[MuiCard]]. */
trait MuiCardProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiCardClasses]
{
  val raised: js.UndefOr[Boolean] = js.undefined
}
object MuiCardProps extends MuiPropsBaseStatic[MuiCardProps]


/** JSON model for [[MuiCardProps.classes]]. */
trait MuiCardClasses extends MuiClassesBase
