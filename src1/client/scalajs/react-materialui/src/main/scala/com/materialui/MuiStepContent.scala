package com.materialui

import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.{Children, JsComponent}

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 16:34
  * @see [[https://material-ui.com/api/step-content/]]
  */
object MuiStepContent {

  val component = JsComponent[MuiStepContentProps, Children.Varargs, Null]( Mui.StepContent )

  def apply(props: MuiStepContentProps = MuiStepContentProps.empty)(children: VdomNode*) = component(props)(children: _*)

}


trait MuiStepContentProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiStepContentClasses]
{
  val TransitionComponent: js.UndefOr[js.Any] = js.undefined
  /** @see [[MuiTransitionDuration]] object. */
  val transitionDuration: js.UndefOr[Double | MuiTransitionDuration | String] = js.undefined
  val TransitionProps: js.UndefOr[js.Object] = js.undefined
}
object MuiStepContentProps extends MuiPropsBaseStatic[MuiStepContentProps]


trait MuiStepContentClasses extends MuiClassesBase {
  val last: js.UndefOr[String] = js.undefined
  val transition: js.UndefOr[String] = js.undefined
}
