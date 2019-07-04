package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react._
import scala.scalajs.js


object MuiLinearProgress {

  val component = JsComponent[MuiLinearProgressProps, Children.None, Null](Mui.LinearProgress)

  def apply(props: MuiLinearProgressProps = MuiLinearProgressProps.empty) =
    component(props)

}


/** JSON для [[MuiLinearProgress]] props. */
trait MuiLinearProgressProps extends MuiPropsBase {
  val classes: js.UndefOr[MuiLinearProgressClasses] = js.undefined
  /** The value of progress, only works in determinate mode. */
  val value: js.UndefOr[Double]                     = js.undefined
  val valueBuffer: js.UndefOr[Double]               = js.undefined
  val color: js.UndefOr[String]                     = js.undefined
  val variant: js.UndefOr[String]                   = js.undefined
}
object MuiLinearProgressProps extends MuiPropsBaseStatic[MuiLinearProgressProps]


/** JSON для [[MuiLinearProgressProps]].classes. */
trait MuiLinearProgressClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val colorPrimary: js.UndefOr[String] = js.undefined
}


/** Допустимые значения для [[MuiLinearProgressProps]].variant. */
object MuiProgressVariants {
  val determinate = "determinate"
  val indeterminate = "indeterminate"
  // linear-only
  val buffer = "buffer"
  val query = "query"
  // circular-only
  val static = "static"
}
