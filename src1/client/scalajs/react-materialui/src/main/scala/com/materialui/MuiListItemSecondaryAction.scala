package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.08.2020 18:33
  * Description: ListItem Secondary Action - экшен справа.
  * @see [[https://material-ui.com/ru/api/list-item-secondary-action/]]
  */
object MuiListItemSecondaryAction {

  val component = JsComponent[MuiListItemSecondaryActionProps, Children.Varargs, Null]( Mui.ListItemSecondaryAction )

  def apply(props: MuiListItemSecondaryActionProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiListItemSecondaryActionProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiListItemSecondaryActionClasses]


trait MuiListItemSecondaryActionClasses
  extends MuiClassesBase
