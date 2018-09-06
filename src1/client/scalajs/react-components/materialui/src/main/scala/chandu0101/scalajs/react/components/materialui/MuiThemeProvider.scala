package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js


object MuiThemeProvider {

  val component = JsComponent[js.Object, Children.Varargs, Null](Mui.MuiThemeProvider)

  def apply(props: MuiThemeProviderProps)(children: VdomElement*) =
    component(props)(children: _*)

}


trait MuiThemeProviderProps extends js.Object {
  val theme:    MuiTheme    // TODO MuiTheme | js.FunctionX

  val disableStylesGeneration: js.UndefOr[Boolean]       = js.undefined
  val sheetsManager: js.UndefOr[js.Object]               = js.undefined

  val key:      js.UndefOr[String]                       = js.undefined
  val ref:      js.UndefOr[MuiThemeProviderM => Unit]    = js.undefined
}


@js.native
trait MuiThemeProviderM extends js.Object {
  def getChildContext(): MuiTheme = js.native
}
