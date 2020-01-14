package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.01.2020 8:18
  * Description: [[https://material-ui.com/api/menu-list/]]
  */
object MuiMenuList {

  val component = JsComponent[MuiMenuListProps, Children.Varargs, Null]( Mui.MenuList )

  def apply(props: MuiMenuListProps)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiMenuListProps
  extends MuiListProps
{
  val autoFocus: js.UndefOr[Boolean] = js.undefined
  val autoFocusItem: js.UndefOr[Boolean] = js.undefined
  val disableListWrap: js.UndefOr[Boolean] = js.undefined
  val variant: js.UndefOr[MuiMenu.Variant] = js.undefined
}
