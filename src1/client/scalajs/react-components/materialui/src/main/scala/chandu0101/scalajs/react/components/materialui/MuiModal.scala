package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js
import scala.scalajs.js.`|`

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.10.18 22:57
  * Description: Mui modal abstract component.
  */
object MuiModal {

  val component = JsComponent[MuiModalProps, Children.Varargs, Null]( Mui.Modal )

  def apply(props: MuiModalProps)(children: VdomNode*) =
    component( props )(children: _*)

}


trait MuiModalPropsBase extends MuiPropsBase {
  val BackdropComponent: js.UndefOr[Component_t] = js.undefined
  val BackdropProps: js.UndefOr[js.Object] = js.undefined
  val container: js.UndefOr[js.Object | js.Function] = js.undefined
  val disableAutoFocus: js.UndefOr[Boolean] = js.undefined
  val disableBackdropClick: js.UndefOr[Boolean] = js.undefined
  val disableEnforceFocus: js.UndefOr[Boolean] = js.undefined
  val disableEscapeKeyDown: js.UndefOr[Boolean] = js.undefined
  val disablePortal: js.UndefOr[Boolean] = js.undefined
  val disableRestoreFocus: js.UndefOr[Boolean] = js.undefined
  val hideBackdrop: js.UndefOr[Boolean] = js.undefined
  val keepMounted: js.UndefOr[Boolean] = js.undefined
  val manager: js.UndefOr[js.Object] = js.undefined
  val onBackdropClick: js.UndefOr[js.Function1[ReactEvent, Unit]] = js.undefined
  val onClose: js.UndefOr[js.Function2[ReactEvent, String, Unit]] = js.undefined
  val onEscapeKeyDown: js.UndefOr[js.Function1[ReactKeyboardEvent, Unit]] = js.undefined
  val onRendered: js.UndefOr[js.Function0[Unit]] = js.undefined
  val open: Boolean
}
trait MuiModalProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiModalClasses]


trait MuiModalClasses extends MuiClassesBase {
  val hidden: js.UndefOr[String] = js.undefined
}

