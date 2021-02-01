package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.VdomNode
import org.scalajs.dom

import scala.scalajs.js
/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.07.19 13:26
  * Description:
  * @see [[https://material-ui.com/api/link/]]
  */
object MuiLink {

  val component = JsForwardRefComponent[MuiLinkProps, Children.Varargs, dom.html.Element](Mui.Link)

  final def apply(props: MuiLinkProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)


  object Underline {
    final def NONE = "none"
    final def HOVER = "hover"
    final def ALWAYS = "always"
  }

}


trait MuiLinkProps
  extends MuiTypoGraphyPropsBase
  with MuiPropsBaseComponent
  with MuiPropsBaseClasses[MuiLinkClasses]
{
  //val color: js.UndefOr[String] = js.undefined
  val TypographyClasses: js.UndefOr[MuiTypoGraphyClasses] = js.undefined
  val underline: js.UndefOr[String] = js.undefined
  //val variant: js.UndefOr[String] = js.undefined
}


trait MuiLinkClasses extends MuiClassesBase {
  val underlineNone: js.UndefOr[String] = js.undefined
  val underlineHover: js.UndefOr[String] = js.undefined
  val underlineAlways: js.UndefOr[String] = js.undefined
  val button: js.UndefOr[String] = js.undefined
  val focusVisible: js.UndefOr[String] = js.undefined
}
