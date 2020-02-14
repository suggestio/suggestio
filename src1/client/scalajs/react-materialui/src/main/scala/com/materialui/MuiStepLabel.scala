package com.materialui

import japgolly.scalajs.react.raw
import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.{Children, JsComponent}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 16:24
  * @see [[https://material-ui.com/api/step-label/]]
  */
object MuiStepLabel {

  val component = JsComponent[MuiStepLabelProps, Children.Varargs, Null]( Mui.StepLabel )

  /** @param children In most cases will simply be a string containing a title for the label. */
  def apply(props: MuiStepLabelProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiStepLabelProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiStepLabelClasses]
{
  val disabled: js.UndefOr[Boolean] = js.undefined
  val error: js.UndefOr[Boolean] = js.undefined
  val icon: js.UndefOr[raw.React.Node] = js.undefined
  val optional: js.UndefOr[raw.React.Node] = js.undefined
  val StepIconComponent: js.UndefOr[js.Any] = js.undefined
  val StepIconProps: js.UndefOr[js.Object] = js.undefined
}


trait MuiStepLabelClasses extends MuiClassesBase {
  val horizontal: js.UndefOr[String] = js.undefined
  val vertical: js.UndefOr[String] = js.undefined
  val label: js.UndefOr[String] = js.undefined
  val active: js.UndefOr[String] = js.undefined
  val completed: js.UndefOr[String] = js.undefined
  val error: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val iconContainer: js.UndefOr[String] = js.undefined
  val alternativeLabel: js.UndefOr[String] = js.undefined
  val labelContainer: js.UndefOr[String] = js.undefined
}
