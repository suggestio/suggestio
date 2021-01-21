package com.materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.08.2020 16:23
  * Description: Mui SvgIcon API.
  */
object MuiSvgIcon {

  implicit class SvgIconApply(icon: MuiSvgIcon) {
    def apply(svgProps: MuiSvgIconProps = MuiPropsBaseStatic.empty)(children: VdomNode*) = {
      val Component = JsComponent[MuiSvgIconProps, Children.Varargs, Null](icon)
      Component(svgProps)(children: _*)
    }
  }

  type FontSize <: String
  object FontSize {
    final def inherit = "inherit".asInstanceOf[FontSize]
    final def large = "large".asInstanceOf[FontSize]
    final def small = "small".asInstanceOf[FontSize]
  }

}

@js.native
trait MuiSvgIcon extends js.Any


trait MuiSvgIconProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiSvgIconClasses]
  with MuiPropsBaseComponent
{
  val color: js.UndefOr[String] = js.undefined
  val fontSize: js.UndefOr[MuiSvgIcon.FontSize] = js.undefined
  val htmlColor: js.UndefOr[String] = js.undefined
  val shapeRendering: js.UndefOr[String] = js.undefined
  val titleAccess: js.UndefOr[String] = js.undefined
  val viewBox: js.UndefOr[String] = js.undefined
}




trait MuiSvgIconClasses extends MuiClassesBase {
  val colorPrimary, colorSecondary, colorAction, colorError, colorDisabled, fontSizeInherit,
      fontSizeSmall, fontSizeLarge: js.UndefOr[String] = js.undefined
}
