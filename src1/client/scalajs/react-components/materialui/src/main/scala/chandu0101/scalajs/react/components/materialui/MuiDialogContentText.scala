package chandu0101.scalajs.react.components.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.10.18 22:29
  * Description: Dialog content text APIs.
  */
object MuiDialogContentText {

  val component = JsComponent[MuiDialogContentTextProps, Children.Varargs, Null](Mui.DialogContentText)

  def apply(props: MuiDialogContentTextProps = MuiDialogContentTextProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON props for [[MuiDialogContentText.component]]. */
trait MuiDialogContentTextProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiDialogContentTextClasses]
object MuiDialogContentTextProps extends MuiPropsBaseStatic[MuiDialogContentTextProps]


/** Props for [[MuiDialogContentTextProps.classes]]. */
trait MuiDialogContentTextClasses
  extends MuiClassesBase
