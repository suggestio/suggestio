package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.10.18 22:29
  * Description: Dialog title APIs.
  */
object MuiDialogTitle {

  val component = JsComponent[MuiDialogTitleProps, Children.Varargs, Null](Mui.DialogTitle)

  def apply(props: MuiDialogTitleProps = MuiDialogTitleProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON props for [[MuiDialogTitle.component]]. */
trait MuiDialogTitleProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiDialogTitleClasses]
{
  val disableTypography: js.UndefOr[Boolean] = js.undefined
}
object MuiDialogTitleProps extends MuiPropsBaseStatic[MuiDialogTitleProps]


/** Props for [[MuiDialogTitleProps.classes]]. */
trait MuiDialogTitleClasses
  extends MuiClassesBase
