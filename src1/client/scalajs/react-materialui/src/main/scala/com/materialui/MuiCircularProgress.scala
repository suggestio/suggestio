package com.materialui

import japgolly.scalajs.react._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.`|`

object MuiCircularProgress {

  val component = JsForwardRefComponent[MuiCircularProgressProps, Children.None, dom.html.Element](Mui.CircularProgress)

  final def apply(props: MuiCircularProgressProps = MuiPropsBaseStatic.empty) =
    component(props)

}


/** JSON для [[MuiCircularProgress]].props. */
trait MuiCircularProgressProps extends js.Object {
  val classes: js.UndefOr[MuiCircularProgressClasses] = js.undefined
  val color: js.UndefOr[String] = js.undefined
  val size: js.UndefOr[Double | String] = js.undefined
  val thickness: js.UndefOr[Double] = js.undefined
  /** between 0 and 100. */
  val value: js.UndefOr[Int] = js.undefined
  val variant: js.UndefOr[String] = js.undefined
}

/** JSON для [[MuiCircularProgressProps]].classes. */
trait MuiCircularProgressClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val static: js.UndefOr[String] = js.undefined
  // TODO ... https://material-ui.com/api/circular-progress/#css-api
}
