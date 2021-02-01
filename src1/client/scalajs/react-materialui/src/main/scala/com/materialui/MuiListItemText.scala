package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.18 17:45
  */
object MuiListItemText {

  val component = JsForwardRefComponent[MuiListItemTextProps, Children.Varargs, dom.html.Element](Mui.ListItemText)

  final def apply(props: MuiListItemTextProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiListItemTextProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiListItemTextClasses]
{
  val disableTypography: js.UndefOr[Boolean] = js.undefined
  val inset: js.UndefOr[Boolean] = js.undefined
  val primary: js.UndefOr[raw.React.Node] = js.undefined
  val primaryTypographyProps: js.UndefOr[js.Object] = js.undefined
  val secondary: js.UndefOr[raw.React.Node] = js.undefined
  val secondaryTypographyProps: js.UndefOr[js.Object] = js.undefined
}


trait MuiListItemTextClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val inset: js.UndefOr[String] = js.undefined
  val dense: js.UndefOr[String] = js.undefined
  val primary: js.UndefOr[String] = js.undefined
  val secondary: js.UndefOr[String] = js.undefined
  val textDense: js.UndefOr[String] = js.undefined
}
