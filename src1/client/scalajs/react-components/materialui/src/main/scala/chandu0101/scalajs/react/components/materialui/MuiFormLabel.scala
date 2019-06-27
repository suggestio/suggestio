package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.06.19 12:45
  * Description: API form [[https://material-ui.com/api/form-label/]].
  */
object MuiFormLabel {

  val component = JsComponent[MuiFormLabelProps, Children.Varargs, Null](Mui.FormLabel)

  def apply(props: MuiFormLabelProps = MuiFormLabelProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiFormLabelProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiFormLabelClasses]
  with MuiPropsBaseComponent
{
  val disabled  : js.UndefOr[Boolean] = js.undefined
  val error     : js.UndefOr[Boolean] = js.undefined
  val filled    : js.UndefOr[Boolean] = js.undefined
  val focused   : js.UndefOr[Boolean] = js.undefined
  val required  : js.UndefOr[Boolean] = js.undefined
}
object MuiFormLabelProps extends MuiPropsBaseStatic[MuiFormLabelProps]


trait MuiFormLabelClasses extends MuiClassesBase {
  val focused: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val error: js.UndefOr[String] = js.undefined
  val filled: js.UndefOr[String] = js.undefined
  val required: js.UndefOr[String] = js.undefined
  val asterisk: js.UndefOr[String] = js.undefined
}
