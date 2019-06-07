package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react.raw
import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.{Children, JsComponent}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 16:09
  * @see [[https://material-ui.com/components/steppers/]]
  */
object MuiStepper {

  val component = JsComponent[MuiStepperProps, Children.Varargs, Null]( Mui.Stepper )

  /** @param children Two or more <Step /> components. */
  def apply(props: MuiStepperProps = MuiStepperProps.empty)(children: VdomNode*) = component(props)(children: _*)

}


/** Properties JSON for [[MuiFormGroup]] component. */
trait MuiStepperProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiStepperClasses]
{
  /** Set the active step (zero based index). */
  val activeStep: js.UndefOr[Int] = js.undefined
  val alternativeLabel: js.UndefOr[Boolean] = js.undefined
  val connector: js.UndefOr[raw.React.Element] = js.undefined
  val nonLinear: js.UndefOr[Boolean] = js.undefined
  val orientation: js.UndefOr[String] = js.undefined
}
object MuiStepperProps extends MuiPropsBaseStatic[MuiStepperProps]

object MuiStepperOrientations {
  def vertical = "vertical"
  def horizontal = "horizontal"
}


/** CSS classes JSON for [[MuiFormGroup]] component props [[MuiFormGroupProps]].classes . */
trait MuiStepperClasses extends MuiClassesBase {
  val horizontal: js.UndefOr[String] = js.undefined
  val vertical: js.UndefOr[String] = js.undefined
  val alternativeLabel: js.UndefOr[String] = js.undefined
}
