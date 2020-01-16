package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.01.2020 8:18
  * Description: [[https://material-ui.com/api/menu-item/]]
  */
object MuiMenuItem {

  val component = JsComponent[MuiMenuItemProps, Children.Varargs, Null]( Mui.MenuItem )

  def apply(props: MuiMenuItemProps = new MuiMenuItemProps {})(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiMenuItemProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiMenuItemClasses]
  with MuiPropsBaseComponent
{
  val dense: js.UndefOr[Boolean] = js.undefined
  val disableGutters: js.UndefOr[Boolean] = js.undefined
  // value: В доках этого поля нет, но селектах - используется.
  val value: js.UndefOr[String] = js.undefined
}


trait MuiMenuItemClasses extends MuiClassesBase {
  val gutters: js.UndefOr[String] = js.undefined
  val selected: js.UndefOr[String] = js.undefined
  val dense: js.UndefOr[String] = js.undefined
}
