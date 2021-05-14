package com.materialui

import japgolly.scalajs.react._
import org.scalajs.dom

import scala.scalajs.js


object MuiLinearProgress {

  val component = JsForwardRefComponent[MuiLinearProgressProps, Children.None, dom.html.Element](Mui.LinearProgress)

  final def apply(props: MuiLinearProgressProps = MuiPropsBaseStatic.empty) =
    component(props)

}


/** Props for [[MuiLinearProgress]]. */
trait MuiLinearProgressProps extends MuiPropsBase {
  val classes: js.UndefOr[MuiLinearProgressClasses] = js.undefined
  /** The value of progress, only works in determinate mode. */
  val value: js.UndefOr[Double]                     = js.undefined
  val valueBuffer: js.UndefOr[Double]               = js.undefined
  val color: js.UndefOr[String]                     = js.undefined
  val variant: js.UndefOr[String]                   = js.undefined
}


/** CSS classes for [[MuiLinearProgressProps]]. */
trait MuiLinearProgressClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val colorPrimary: js.UndefOr[String] = js.undefined
}


/** Enum values for [[MuiLinearProgressProps]].variant field. */
object MuiProgressVariants {
  val determinate = "determinate"
  val indeterminate = "indeterminate"
  // linear-only
  val buffer = "buffer"
  val query = "query"
}
