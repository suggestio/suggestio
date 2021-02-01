package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom

import scala.scalajs.js

    
object MuiAvatar {

  val component = JsForwardRefComponent[js.Object, Children.Varargs, dom.html.Element](Mui.Avatar)

  final def apply(props: MuiAvatarProps = MuiPropsBaseStatic.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON for [[MuiAvatar]] props. */
trait MuiAvatarProps
  extends MuiPropsBase
  with MuiPropsBaseComponent
{
  val alt: js.UndefOr[String] = js.undefined
  val classes: js.UndefOr[MuiAvatarClasses] = js.undefined
  val imgProps: js.UndefOr[js.Object] = js.undefined
  val sizes: js.UndefOr[String] = js.undefined
  val src: js.UndefOr[String] = js.undefined
  val srcSet: js.UndefOr[String] = js.undefined
}


/** JSON for [[MuiAvatarProps.classes]]. */
trait MuiAvatarClasses extends MuiClassesBase {
  val colorDefault: js.UndefOr[String] = js.undefined
  val img: js.UndefOr[String] = js.undefined
}

