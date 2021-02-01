package com.materialui

import japgolly.scalajs.react._
import org.scalajs.dom

import scala.scalajs.js


object MuiLinearProgress {

  val component = JsForwardRefComponent[MuiLinearProgressProps, Children.None, dom.html.Element](Mui.LinearProgress)

  final def apply(props: MuiLinearProgressProps = MuiPropsBaseStatic.empty) =
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
}
