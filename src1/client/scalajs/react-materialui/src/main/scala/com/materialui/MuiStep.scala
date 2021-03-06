package com.materialui

import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.{Children, JsForwardRefComponent}
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 16:16
  * @see [[https://material-ui.com/api/step/]]
  */
object MuiStep {

  val component = JsForwardRefComponent[MuiStepProps, Children.Varargs, dom.html.Element]( Mui.Step )

  /** @param children Should be Step sub-components such as [[MuiStepLabel]], [[MuiStepContent]]. */
  def apply(props: MuiStepProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiStepProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiStepClasses]
{
  val active: js.UndefOr[Boolean] = js.undefined
  val completed: js.UndefOr[Boolean] = js.undefined
  val disabled: js.UndefOr[Boolean] = js.undefined
}


trait MuiStepClasses extends MuiClassesBase {
  val horizontal: js.UndefOr[String] = js.undefined
  val vertical: js.UndefOr[String] = js.undefined
  val alternativeLabel: js.UndefOr[String] = js.undefined
  val completed: js.UndefOr[String] = js.undefined
}
