package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import scala.scalajs.js
import scala.scalajs.js.`|`


object MuiChip {

  val component = JsComponent[MuiChipProps, Children.None, Null](Mui.Chip)

  def apply(props: MuiChipProps) =
    component(props)

}


/** React component props for [[MuiChip]]. */
trait MuiChipProps extends MuiPropsBase with MuiPropsBaseComponent {
  val avatar: js.UndefOr[React.Element] = js.undefined
  val classes: js.UndefOr[MuiChipClasses] = js.undefined
  val clickable: js.UndefOr[Boolean] = js.undefined
  /** See [[MuiColorTypes]] except 'inherit'. */
  val color: js.UndefOr[String] = js.undefined
  val deleteIcon: js.UndefOr[React.Element] = js.undefined
  val label: js.UndefOr[React.Node] = js.undefined
  val onDelete: js.UndefOr[js.Function1[ReactUIEventFromHtml, _]] = js.undefined
  val variant: js.UndefOr[String] = js.undefined
}
object MuiChipProps extends MuiPropsBaseStatic[MuiChipProps]


/** Values for [[MuiChipProps.variant]]. */
object MuiChipVariants {
  val default = "default"
  val outlined = "outlined"
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
