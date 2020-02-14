package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.09.18 15:46
  * Description: MaterialUI Input label.
  */
object MuiInputLabel {

  val component = JsComponent[MuiInputLabelProps, Children.Varargs, Null](Mui.InputLabel)

  final def apply(props: MuiInputLabelProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiInputLabelProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiInputLabelClasses]
{
  val disableAnimation: js.UndefOr[Boolean] = js.undefined
  val disabled: js.UndefOr[Boolean] = js.undefined
  val error: js.UndefOr[Boolean] = js.undefined
  val focused: js.UndefOr[Boolean] = js.undefined
  val margin: js.UndefOr[String] = js.undefined
  val required: js.UndefOr[Boolean] = js.undefined
  val shrink: js.UndefOr[Boolean] = js.undefined
}


trait MuiInputLabelClasses extends MuiFormLabelClasses {
  val formControl: js.UndefOr[String] = js.undefined
  val marginDense: js.UndefOr[String] = js.undefined
  val shrink: js.UndefOr[String] = js.undefined
  val animated: js.UndefOr[String] = js.undefined
}
