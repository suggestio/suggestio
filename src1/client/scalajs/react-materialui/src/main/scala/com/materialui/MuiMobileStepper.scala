package com.materialui

import japgolly.scalajs.react.{Children, JsComponent, JsForwardRefComponent, raw}
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.06.19 10:16
  * Description: Simple mobile stepper API.
  */
object MuiMobileStepper {

  val component = JsForwardRefComponent[MuiMobileStepperProps, Children.None, dom.html.Element]( Mui.MobileStepper )

  def apply(props: MuiMobileStepperProps) =
    component(props)

}


trait MuiMobileStepperProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiMobileStepperClasses]
{
  val activeStep: js.UndefOr[Int] = js.undefined
  val backButton: js.UndefOr[raw.React.Node] = js.undefined
  val LinearProgressProps: js.UndefOr[MuiLinearProgressProps] = js.undefined
  val nextButton: js.UndefOr[raw.React.Node] = js.undefined
  val position: js.UndefOr[String] = js.undefined
  val steps: Int
  val variant: js.UndefOr[String] = js.undefined
}


trait MuiMobileStepperClasses extends MuiClassesBase {
  val positionBottom: js.UndefOr[String] = js.undefined
  val positionTop: js.UndefOr[String] = js.undefined
  val positionStatic: js.UndefOr[String] = js.undefined
  val dots: js.UndefOr[String] = js.undefined
  val dot: js.UndefOr[String] = js.undefined
  val dotActive: js.UndefOr[String] = js.undefined
  val progress: js.UndefOr[String] = js.undefined
}


object MuiMobileStepperPositions {
  def bottom = "bottom"
  def top = "top"
  def static = "static"
}


object MuiMobileStepperVariants {
  def text = "text"
  def dots = "dots"
  def progress = "progress"
}
