package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.09.18 16:24
  */
object MuiFormControl {

  val component = JsForwardRefComponent[MuiFormControlProps, Children.Varargs, dom.html.Element](Mui.FormControl)

  final def apply(props: MuiFormControlProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiFormControlProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiFormControlClasses]
  with MuiPropsBaseComponent
{
  val disabled: js.UndefOr[Boolean] = js.undefined
  val error: js.UndefOr[Boolean] = js.undefined
  val fullWidth: js.UndefOr[Boolean] = js.undefined
  val margin: js.UndefOr[String] = js.undefined
  val required: js.UndefOr[Boolean] = js.undefined
}


trait MuiFormControlClasses extends MuiClassesBase {
  val marginNormal: js.UndefOr[String] = js.undefined
  val marginDense: js.UndefOr[String] = js.undefined
  val fullWidth: js.UndefOr[String] = js.undefined
}
