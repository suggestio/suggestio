package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js


object MuiIconButton {

  val component = JsComponent[MuiIconButtonProps, Children.Varargs, Null](Mui.IconButton)

  def apply(props: MuiIconButtonProps)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON для [[MuiIconButton]] props. */
trait MuiIconButtonProps extends MuiPropsBase {
  val classes: js.UndefOr[MuiIconButtonClasses] = js.undefined
  val color: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[Boolean] = js.undefined
  val disableRipple: js.UndefOr[Boolean] = js.undefined
}


/** JSON для [[MuiIconButtonProps.classes]]. */
trait MuiIconButtonClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val colorInherit: js.UndefOr[String] = js.undefined
  val colorPrimary: js.UndefOr[String] = js.undefined
  val colorSecondary: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val label: js.UndefOr[String] = js.undefined
}


@js.native
trait MuiIconButtonM extends js.Object {
  def hideTooltip(): Unit = js.native

  def setKeyboardFocus(): Unit = js.native

  def showTooltip(): Unit = js.native
}
