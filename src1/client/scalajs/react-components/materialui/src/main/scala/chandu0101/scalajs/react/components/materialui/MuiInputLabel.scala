package chandu0101.scalajs.react.components.materialui

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

  def apply(props: MuiInputLabelProps = MuiInputLabelProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiInputLabelProps extends MuiPropsBase {
  val classes: js.UndefOr[MuiInputLabelClasses] = js.undefined
  val disableAnimation: js.UndefOr[Boolean] = js.undefined
  val disabled: js.UndefOr[Boolean] = js.undefined
  val error: js.UndefOr[Boolean] = js.undefined
  val focused: js.UndefOr[Boolean] = js.undefined
  val FormLabelClasses: js.UndefOr[js.Object] = js.undefined
  val margin: js.UndefOr[String] = js.undefined
  val required: js.UndefOr[Boolean] = js.undefined
  val shrink: js.UndefOr[Boolean] = js.undefined
}
object MuiInputLabelProps extends MuiPropsBaseStatic[MuiInputLabelProps]


trait MuiInputLabelClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val formControl: js.UndefOr[String] = js.undefined
  val marginDense: js.UndefOr[String] = js.undefined
  val shrink: js.UndefOr[String] = js.undefined
  val animated: js.UndefOr[String] = js.undefined
}
