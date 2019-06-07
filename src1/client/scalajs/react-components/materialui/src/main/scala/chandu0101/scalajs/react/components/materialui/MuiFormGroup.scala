package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react.vdom.VdomNode
import japgolly.scalajs.react.{Children, JsComponent}

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.19 21:07
  * Description: Form Group API.
  * @see [[https://material-ui.com/api/form-group/]]
  */
object MuiFormGroup {

  val component = JsComponent[MuiFormGroupProps, Children.Varargs, Null]( Mui.FormGroup )

  def apply(props: MuiFormGroupProps = MuiFormGroupProps.empty)(children: VdomNode*) = component(props)(children: _*)

}


/** Properties JSON for [[MuiFormGroup]] component. */
trait MuiFormGroupProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiFormGroupClasses]
{
  val row: js.UndefOr[Boolean] = js.undefined
}
object MuiFormGroupProps extends MuiPropsBaseStatic[MuiFormGroupProps]


/** CSS classes JSON for [[MuiFormGroup]] component props [[MuiFormGroupProps]].classes . */
trait MuiFormGroupClasses extends MuiClassesBase {
  val row: js.UndefOr[String] = js.undefined
}
