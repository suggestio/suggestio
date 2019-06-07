package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react.{Children, JsComponent}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.06.19 16:34
  * @see [[https://material-ui.com/api/step-content/]]
  */
object MuiStepConnector {

  val component = JsComponent[MuiStepConnectorProps, Children.None, Null]( Mui.StepConnector )

  def apply(props: MuiStepConnectorProps = MuiStepConnectorProps.empty) = component(props)

}


trait MuiStepConnectorProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiStepConnectorClasses]
object MuiStepConnectorProps extends MuiPropsBaseStatic[MuiStepConnectorProps]

trait MuiStepConnectorClasses extends MuiClassesBase {
  val horizontal: js.UndefOr[String] = js.undefined
  val vertical: js.UndefOr[String] = js.undefined
  val alternativeLabel: js.UndefOr[String] = js.undefined
  val active: js.UndefOr[String] = js.undefined
  val completed: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val line: js.UndefOr[String] = js.undefined
  val lineHorizontal: js.UndefOr[String] = js.undefined
  val lineVertical: js.UndefOr[String] = js.undefined
}
