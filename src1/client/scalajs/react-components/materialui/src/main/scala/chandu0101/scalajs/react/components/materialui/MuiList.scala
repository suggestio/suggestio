package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js


object MuiList {

  val component = JsComponent[js.Object, Children.Varargs, Null](Mui.List)

  /**
    * @param children These are usually `ListItem`s that are passed to be part of the list.
    */
  def apply(props: MuiListProps = new MuiListProps {})(children: VdomNode*) = {
    component(props)(children: _*)
  }

}


trait MuiListProps extends js.Object {
  val key:      js.UndefOr[String]        = js.undefined
  val ref:      js.UndefOr[String]        = js.undefined
  /** Override the inline-styles of the root element. */
  val style:    js.UndefOr[CssProperties] = js.undefined
}
