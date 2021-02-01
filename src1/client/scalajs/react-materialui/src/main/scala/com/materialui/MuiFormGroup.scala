package com.materialui

import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.{Children, JsComponent, JsForwardRefComponent}
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.19 21:07
  * Description: Form Group API.
  * @see [[https://material-ui.com/api/form-group/]]
  */
object MuiFormGroup {

  val component = JsForwardRefComponent[MuiFormGroupProps, Children.Varargs, dom.html.Element]( Mui.FormGroup )

  final def apply(props: MuiFormGroupProps = MuiPropsBaseStatic.empty)(children: VdomNode*) = component(props)(children: _*)

}


/** Properties JSON for [[MuiFormGroup]] component. */
trait MuiFormGroupProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiFormGroupClasses]
{
  val row: js.UndefOr[Boolean] = js.undefined
}


/** CSS classes JSON for [[MuiFormGroup]] component props [[MuiFormGroupProps]].classes . */
trait MuiFormGroupClasses extends MuiClassesBase {
  val row: js.UndefOr[String] = js.undefined
}
