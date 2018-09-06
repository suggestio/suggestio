package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._
import scala.scalajs.js


object MuiToolbarTitle {

  val component = JsComponent[js.Object, Children.None, Null](Mui.ToolbarTitle)

  def apply(props: MuiToolbarTitleProps) = {
    component(props)
  }

}


trait MuiToolbarTitleProps extends js.Object {
  val key:       js.UndefOr[String]        = js.undefined
  val ref:       js.UndefOr[String]        = js.undefined
  /** The css class name of the root element. */
  val className: js.UndefOr[String]        = js.undefined
  /** Override the inline-styles of the root element. */
  val style:     js.UndefOr[CssProperties] = js.undefined
  /** The text to be displayed. */
  val text:      js.UndefOr[VdomNode]      = js.undefined
}

