package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.10.18 22:29
  * Description: Dialog content APIs.
  */
object MuiDialogActions {

  val component = JsComponent[MuiDialogActionsProps, Children.Varargs, Null](Mui.DialogActions)

  def apply(props: MuiDialogActionsProps = MuiDialogActionsProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON props for [[MuiDialogActions.component]]. */
trait MuiDialogActionsProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiDialogActionsClasses]
{
  val disableActionSpacing: js.UndefOr[Boolean] = js.undefined
}
object MuiDialogActionsProps extends MuiPropsBaseStatic[MuiDialogActionsProps]


/** Props for [[MuiDialogActionsProps.classes]]. */
trait MuiDialogActionsClasses
  extends MuiClassesBase
{
  val action: js.UndefOr[Boolean] = js.undefined
}
