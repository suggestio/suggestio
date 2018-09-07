package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import japgolly.scalajs.react.vdom._

import scala.scalajs.js
import scala.scalajs.js.|


object MuiList {

  val component = JsComponent[MuiListProps, Children.Varargs, Null](Mui.List)

  /** @param children These are usually `ListItem`s that are passed to be part of the list. */
  def apply(props: MuiListProps = MuiListProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


/** JSON для [[MuiList]] props. */
trait MuiListProps extends MuiPropsBase {
  val classes:        js.UndefOr[MuiListClasses] = js.undefined
  val component:      js.UndefOr[String | js.Object] = js.undefined
  val dense:          js.UndefOr[Boolean] = js.undefined
  val disablePadding: js.UndefOr[Boolean] = js.undefined
  val subheader:      js.UndefOr[React.Node] = js.undefined
}
object MuiListProps extends MuiPropsBaseStatic[MuiListProps]


/** JSON для [[MuiListProps.classes]]. */
trait MuiListClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val padding: js.UndefOr[String] = js.undefined
  val dense: js.UndefOr[String] = js.undefined
  val subheader: js.UndefOr[String] = js.undefined
}
