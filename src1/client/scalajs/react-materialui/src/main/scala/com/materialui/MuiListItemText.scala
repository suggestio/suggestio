package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.18 17:45
  */
object MuiListItemText {

  val component = JsComponent[MuiListItemTextProps, Children.Varargs, Null](Mui.ListItemText)

  def apply(props: MuiListItemTextProps = new MuiListItemTextProps {})(children: VdomNode*) =
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
object MuiListItemTextProps extends MuiPropsBaseStatic[MuiListItemTextProps]


trait MuiListItemTextClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val inset: js.UndefOr[String] = js.undefined
  val dense: js.UndefOr[String] = js.undefined
  val primary: js.UndefOr[String] = js.undefined
  val secondary: js.UndefOr[String] = js.undefined
  val textDense: js.UndefOr[String] = js.undefined
}
