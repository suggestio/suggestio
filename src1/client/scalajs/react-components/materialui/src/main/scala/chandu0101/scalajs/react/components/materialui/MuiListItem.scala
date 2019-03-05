package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.raw.React
import japgolly.scalajs.react.vdom._

import scala.scalajs.js
import scala.scalajs.js.`|`


object MuiListItem {

  val component = JsComponent[MuiListItemProps, Children.Varargs, Null](Mui.ListItem)

  /** @param children Children passed into the `ListItem`. */
  def apply(props: MuiListItemProps = MuiListItemProps.empty)(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiListItemProps extends MuiPropsBase {
  val button:                      js.UndefOr[Boolean]                                = js.undefined
  val classes:                     js.UndefOr[MuiListItemClasses]                     = js.undefined
  val selected:                    js.UndefOr[Boolean]                                = js.undefined
  val divider:                     js.UndefOr[Boolean]                                = js.undefined
  val disabled:                    js.UndefOr[Boolean]                                = js.undefined
  val dense:                       js.UndefOr[Boolean]                                = js.undefined
  val disableGutters:              js.UndefOr[Boolean]                                = js.undefined

  val component:                   js.UndefOr[String | React.Element]                 = js.undefined
  val ContainerComponent:          js.UndefOr[String | React.Element]                 = js.undefined
}
object MuiListItemProps extends MuiPropsBaseStatic[MuiListItemProps]


trait MuiListItemClasses extends js.Object {
  val root: js.UndefOr[String] = js.undefined
  val container: js.UndefOr[String] = js.undefined
  val focusVisible: js.UndefOr[String] = js.undefined
  val dense: js.UndefOr[String] = js.undefined
  val disabled: js.UndefOr[String] = js.undefined
  val divider: js.UndefOr[String] = js.undefined
  val gutters: js.UndefOr[String] = js.undefined
  val button: js.UndefOr[String] = js.undefined
  val secondaryAction: js.UndefOr[String] = js.undefined
  val selected: js.UndefOr[String] = js.undefined
}
