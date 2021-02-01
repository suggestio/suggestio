package com.materialui

import japgolly.scalajs.react._
import org.scalajs.dom

import scala.scalajs.js


object MuiChip {

  val component = JsForwardRefComponent[MuiChipProps, Children.None, dom.html.Element](Mui.Chip)

  final def apply(props: MuiChipProps = MuiPropsBaseStatic.empty) =
    component(props)

  type Variant <: String
  /** Values for [[MuiChipProps.variant]]. */
  object Variant {
    final def FILLED = "filled".asInstanceOf[Variant]
    final def OUTLINED = "outlined".asInstanceOf[Variant]
  }

}


/** React component props for [[MuiChip]]. */
trait MuiChipProps
  extends MuiPropsBase
  with MuiPropsBaseComponent
  with MuiPropsBaseClasses[MuiChipClasses]
{
  val avatar: js.UndefOr[raw.React.Element] = js.undefined
  val clickable: js.UndefOr[Boolean] = js.undefined
  /** See [[MuiColorTypes]] except 'inherit'. */
  val color: js.UndefOr[String] = js.undefined
  val deleteIcon: js.UndefOr[raw.React.Element] = js.undefined
  val disabled: js.UndefOr[Boolean] = js.undefined
  val icon: js.UndefOr[raw.React.Element] = js.undefined
  val label: js.UndefOr[raw.React.Node] = js.undefined
  val onDelete: js.UndefOr[js.Function1[ReactUIEventFromHtml, _]] = js.undefined
  /** @see [[MuiRadioSizes]] */
  val size: js.UndefOr[String] = js.undefined
  val variant: js.UndefOr[MuiChip.Variant] = js.undefined
}


/** CSS styles for [[MuiChipProps.classes]]. */
trait MuiChipClasses extends MuiClassesBase {
  val colorPrimary: js.UndefOr[String] = js.undefined
  val colorSecondary: js.UndefOr[String] = js.undefined
  val clickable: js.UndefOr[String] = js.undefined
  val clickableColorPrimary: js.UndefOr[String] = js.undefined
  val clickableColorSecondary: js.UndefOr[String] = js.undefined
  val deletable: js.UndefOr[String] = js.undefined
  val deletableColorPrimary: js.UndefOr[String] = js.undefined
  val deletableColorSecondary: js.UndefOr[String] = js.undefined
  val outlined: js.UndefOr[String] = js.undefined
  val outlinedPrimary: js.UndefOr[String] = js.undefined
  val outlinedSecondary: js.UndefOr[String] = js.undefined
  val avatar: js.UndefOr[String] = js.undefined
  val avatarColorPrimary: js.UndefOr[String] = js.undefined
  val avatarColorSecondary: js.UndefOr[String] = js.undefined
  val avatarChildren: js.UndefOr[String] = js.undefined
  val label: js.UndefOr[String] = js.undefined
  val deleteIcon: js.UndefOr[String] = js.undefined
  val deleteIconColorPrimary: js.UndefOr[String] = js.undefined
  val deleteIconColorSecondary: js.UndefOr[String] = js.undefined
  val deleteIconOutlinedColorPrimary: js.UndefOr[String] = js.undefined
  val deleteIconOutlinedColorSecondary: js.UndefOr[String] = js.undefined
}
