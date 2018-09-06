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
    
case class MuiGridList(
  key:        js.UndefOr[String]        = js.undefined,
  ref:        js.UndefOr[String]        = js.undefined,
  /** Number of px for one cell height.
     You can set `'auto'` if you want to let the children determine the height. */
  cellHeight: js.UndefOr[Int]           = js.undefined,
  /** Number of columns. */
  cols:       js.UndefOr[Double]        = js.undefined,
  /** Number of px for the padding/spacing between items. */
  padding:    js.UndefOr[Double]        = js.undefined,
  /** Override the inline-styles of the root element. */
  style:      js.UndefOr[CssProperties] = js.undefined){

  /**
    * @param children Grid Tiles that will be in Grid List.
   */
  def apply(children: VdomNode*) = {
    
    val props = JSMacro[MuiGridList](this)
    val f = JsComponent[js.Object, Children.Varargs, Null](Mui.GridList)
    f(props)(children: _*)
  }
}
