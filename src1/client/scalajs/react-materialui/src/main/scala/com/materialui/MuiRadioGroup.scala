package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.08.2020 8:56
  * Description: API for radio-buttons group.
  */
object MuiRadioGroup {

  val component = JsForwardRefComponent[MuiRadioGroupProps, Children.Varargs, dom.html.Element]( Mui.RadioGroup )

  def apply(props: MuiRadioGroupProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiRadioGroupProps
  extends MuiFormGroupProps
{
  // Uncontrolled
  val defaultValue: js.UndefOr[js.Array[String] | Double | String] = js.undefined
  val name: js.UndefOr[String] = js.undefined
  val value: js.UndefOr[String] = js.undefined
}