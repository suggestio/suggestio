package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js


object MuiPaper {

  val component = JsComponent[js.Object, Children.Varargs, Null](Mui.Paper)

  /** @param children Children passed into the paper element. */
  def apply(props: MuiPaperProps = new MuiPaperProps {})(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiPaperProps extends js.Object {
  val key:               js.UndefOr[String]        = js.undefined
  val ref:               js.UndefOr[String]        = js.undefined
  /** Set to true to generate a circular paper container. */
  val circle:            js.UndefOr[Boolean]       = js.undefined
  /** By default, the paper container will have a border radius.
     Set this to false to generate a container with sharp corners. */
  val rounded:           js.UndefOr[Boolean]       = js.undefined
  /** Override the inline-styles of the root element. */
  val style:             js.UndefOr[CssProperties] = js.undefined
  /** Set to false to disable CSS transitions for the paper element. */
  val transitionEnabled: js.UndefOr[Boolean]       = js.undefined
  /** This number represents the zDepth of the paper shadow. */
  val zDepth:            js.UndefOr[ZDepth]        = js.undefined
}
