package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.01.2020 8:18
  * Description: [[https://material-ui.com/api/menu/]]
  */
object MuiMenu {

  val component = JsForwardRefComponent[MuiMenuProps, Children.Varargs, dom.html.Element]( Mui.Menu )

  def apply(props: MuiMenuProps)(children: VdomNode*) =
    component(props)(children: _*)

  type Variant <: js.Any
  object Variants {
    def menu = "menu".asInstanceOf[Variant]
    def selectedMenu = "selectedMenu".asInstanceOf[Variant]
  }

}


trait MuiMenuPropsBase
  extends MuiPopOverPropsBase
  with MuiPropsBaseClasses[MuiMenuClasses]
{
  val autoFocus: js.UndefOr[Boolean] = js.undefined
  val disableAutoFocusItem: js.UndefOr[Boolean] = js.undefined
  val MenuListProps: js.UndefOr[MuiMenuListProps] = js.undefined
  @JSName("onClose")
  val onClose2: js.UndefOr[js.Function2[ReactEvent, String, Unit]] = js.undefined
  val PopoverClasses: js.UndefOr[js.Object/*TODO заменить Object на MuiPopoverClasses */] = js.undefined
  val variant: js.UndefOr[MuiMenu.Variant] = js.undefined
}


trait MuiMenuProps
  extends MuiPopOverPropsBaseOpen
  with MuiMenuPropsBase


trait MuiMenuClasses extends MuiPopOverClasses {
  val list: js.UndefOr[String] = js.undefined
}
