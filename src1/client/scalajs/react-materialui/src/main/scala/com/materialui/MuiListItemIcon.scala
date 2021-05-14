package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.18 17:34
  * Description: A simple wrapper to apply List styles to an Icon or SvgIcon.
  */
object MuiListItemIcon {

  val component = JsForwardRefComponent[MuiListItemIconProps, Children.Varargs, dom.html.Element](Mui.ListItemIcon)

  final def apply(props: MuiListItemIconProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** Props for [[MuiListItem]]. */
trait MuiListItemIconProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiListItemIconClasses]


/** CSS Clases for [[MuiListItemIconProps.classes]]. */
trait MuiListItemIconClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
}
