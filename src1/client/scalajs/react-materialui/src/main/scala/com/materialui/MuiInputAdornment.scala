package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.09.18 17:51
  */
object MuiInputAdornment {

  val component = JsForwardRefComponent[MuiInputAdornmentProps, Children.Varargs, dom.html.Element](Mui.InputAdornment)

  final def apply(props: MuiInputAdornmentProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiInputAdornmentProps extends MuiPropsBase {
  val classes: js.UndefOr[MuiInputAdornmentClasses] = js.undefined
  val component: js.UndefOr[String | js.Function | js.Object] = js.undefined
  val disableTypography: js.UndefOr[Boolean] = js.undefined
  val position: js.UndefOr[String] = js.undefined
}


trait MuiInputAdornmentClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val positionStart: js.UndefOr[String] = js.undefined
  val positionEnd: js.UndefOr[String] = js.undefined
}


object MuiInputAdornmentPositions {
  val start = "start"
  val end = "end"
}
