package chandu0101.scalajs.react.components
package materialui

import chandu0101.macros.tojs.JSMacro
import japgolly.scalajs.react._
import japgolly.scalajs.react.raw._
import japgolly.scalajs.react.vdom._
import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.`|`

/**
 * This file is generated - submit issues instead of PR against it
 */
    
case class MuiSubheader(
  key:      js.UndefOr[String]        = js.undefined,
  ref:      js.UndefOr[String]        = js.undefined,
  /** If true, the `Subheader` will be indented. */
  inset:    js.UndefOr[Boolean]       = js.undefined,
  /** Override the inline-styles of the root element. */
  style:    js.UndefOr[CssProperties] = js.undefined){

  /**
    * @param children Node that will be placed inside the `Subheader`.
   */
  def apply(children: VdomNode*) = {
    
    val props = JSMacro[MuiSubheader](this)
    val f = JsComponent[js.Object, Children.Varargs, Null](Mui.Subheader)
    f(props)(children: _*)
  }
}
