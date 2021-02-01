package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js


object MuiCardHeader {

  val component = JsForwardRefComponent[MuiCardHeaderProps, Children.Varargs, dom.html.Element](Mui.CardHeader)

  final def apply(props: MuiCardHeaderProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiCardHeaderProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiCardHeaderClasses]
  with MuiPropsBaseComponent
{
  val action: js.UndefOr[raw.React.Node] = js.undefined
  val avatar: js.UndefOr[raw.React.Node] = js.undefined
  val disableTypography: js.UndefOr[Boolean] = js.undefined
  val subheader: js.UndefOr[raw.React.Node] = js.undefined
  val subheaderTypographyProps: js.UndefOr[MuiTypoGraphyProps] = js.undefined
  val title: js.UndefOr[raw.React.Node] = js.undefined
  val titleTypographyProps: js.UndefOr[MuiTypoGraphyProps] = js.undefined
}


trait MuiCardHeaderClasses extends MuiClassesBase {
  val avatar: js.UndefOr[String] = js.undefined
  val action: js.UndefOr[String] = js.undefined
  val content: js.UndefOr[String] = js.undefined
  val title: js.UndefOr[String] = js.undefined
  val subheader: js.UndefOr[String] = js.undefined
}

