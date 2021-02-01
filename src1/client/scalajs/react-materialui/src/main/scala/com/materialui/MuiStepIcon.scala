package com.materialui

import japgolly.scalajs.react.{Children, JsForwardRefComponent, raw}
import japgolly.scalajs.react.vdom.VdomNode
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 16:34
  * @see [[https://material-ui.com/api/step-content/]]
  */
object MuiStepIcon {

  val component = JsForwardRefComponent[MuiStepIconProps, Children.Varargs, dom.html.Element]( Mui.StepIcon )

  /** @param children In most cases will simply be a string containing a title for the label. */
  def apply(props: MuiStepIconProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiStepIconProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiStepIconClasses]
{
  val icon: raw.React.Node
  val active: js.UndefOr[Boolean] = js.undefined
  val completed: js.UndefOr[Boolean] = js.undefined
  val error: js.UndefOr[Boolean] = js.undefined
}


trait MuiStepIconClasses extends MuiClassesBase {
  val text: js.UndefOr[String] = js.undefined
  val active: js.UndefOr[String] = js.undefined
  val completed: js.UndefOr[String] = js.undefined
  val error: js.UndefOr[String] = js.undefined
}
