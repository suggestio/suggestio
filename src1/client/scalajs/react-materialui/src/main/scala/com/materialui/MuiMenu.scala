package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.01.2020 8:18
  * Description: [[https://material-ui.com/api/menu/]]
  */
object MuiMenu {

  val component = JsComponent[MuiMenuProps, Children.Varargs, Null]( Mui.Menu )

  def apply(props: MuiMenuProps)(children: VdomNode*) =
    component(props)(children: _*)

  type Variant <: js.Any
  object Variants {
    def menu = "menu".asInstanceOf[Variant]
    def selectedMenu = "selectedMenu".asInstanceOf[Variant]
  }

}


trait MuiMenuProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiMenuClasses]
{
  val anchorEl: js.UndefOr[js.Object | js.Function] = js.undefined
  val autoFocus: js.UndefOr[Boolean] = js.undefined
  val disableAutoFocusItem: js.UndefOr[Boolean] = js.undefined
  val MenuListProps: js.UndefOr[js.Object] = js.undefined
  val open: Boolean
  val onClose: js.UndefOr[js.Function2[ReactEvent, String, Unit]] = js.undefined
  val PopoverClasses: js.UndefOr[js.Object/*TODO заменить Object на MuiPopoverClasses */] = js.undefined
  val transitionDuration: js.UndefOr[MuiTransitionDuration] = js.undefined
  val variant: js.UndefOr[MuiMenu.Variant] = js.undefined
}


trait MuiMenuClasses extends MuiClassesBase {
  val paper: js.UndefOr[String] = js.undefined
  val list: js.UndefOr[String] = js.undefined
}
