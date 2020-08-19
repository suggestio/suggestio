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

}

@js.native
trait MuiSvgIcon extends js.Any


trait MuiSvgIconProps
  extends MuiPropsBase
  with MuiPropsBaseClasses[MuiSvgIconClasses]
  with MuiPropsBaseComponent
{
  val color: js.UndefOr[String] = js.undefined
  val fontSize: js.UndefOr[String] = js.undefined
  val htmlColor: js.UndefOr[String] = js.undefined
  val shapeRendering: js.UndefOr[String] = js.undefined
  val titleAccess: js.UndefOr[String] = js.undefined
  val viewBox: js.UndefOr[String] = js.undefined
}


object MuiFontSizes {
  final def default = "default"
  final def inherit = "inherit"
  final def large = "large"
  final def small = "small"
}

trait MuiSvgIconClasses extends MuiClassesBase {
  val colorPrimary, colorSecondary, colorAction, colorError, colorDisabled, fontSizeInherit,
      fontSizeSmall, fontSizeLarge: js.UndefOr[String] = js.undefined
}
