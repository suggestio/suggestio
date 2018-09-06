package chandu0101.scalajs.react.components
package materialui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom._

import scala.scalajs.js
import scala.scalajs.js.`|`

    
object MuiListItem {

  val component = JsComponent[js.Object, Children.Varargs, Null](Mui.ListItem)

  /** @param children Children passed into the `ListItem`. */
  def apply(props: MuiListItemProps = new MuiListItemProps {})(children: VdomNode*) =
    component(props)(children: _*)

}


trait MuiListItemProps extends js.Object {
  val button:                      js.UndefOr[Boolean]                                = js.undefined
  val classes:                     js.UndefOr[MuiListItemClasses]                     = js.undefined
  val selected:                    js.UndefOr[Boolean]                                = js.undefined
  val divider:                     js.UndefOr[Boolean]                                = js.undefined
  val disabled:                    js.UndefOr[Boolean]                                = js.undefined
  val dense:                       js.UndefOr[Boolean]                                = js.undefined
  val disableGutters:              js.UndefOr[Boolean]                                = js.undefined

  val component:                   js.UndefOr[String | VdomElement]                   = js.undefined
  val ContainerComponent:          js.UndefOr[String | VdomElement]                   = js.undefined

  /** Unit function fired when the list item is clicked.
     @param {object} event Click event targeting the list item. */
  val onClick:                     js.UndefOr[js.Function1[ReactEvent, Unit]]                     = js.undefined
  /** Unit function fired when the `ListItem` is focused or blurred by the keyboard.
     @param {object} event `focus` or `blur` event targeting the `ListItem`.
     @param {boolean} isKeyboardFocused If true, the `ListItem` is focused. */
  val onKeyboardFocus:             js.UndefOr[js.Function2[ReactFocusEvent, Boolean, Unit]]       = js.undefined
  val onMouseEnter:                js.UndefOr[js.Function1[ReactMouseEvent, Unit]]                = js.undefined
  val onMouseLeave:                js.UndefOr[js.Function1[ReactMouseEvent, Unit]]                = js.undefined
  /** Unit function fired when the `ListItem` toggles its nested list.
     @param {object} listItem The `ListItem`. */
  val onNestedListToggle:          js.UndefOr[js.Function1[js.Any, Unit]]                         = js.undefined
  val onTouchEnd:                  js.UndefOr[js.Function1[ReactTouchEvent, Unit]]                = js.undefined
  val onTouchStart:                js.UndefOr[js.Function1[ReactTouchEvent, Unit]]                = js.undefined
  /** (Passed on to EnhancedButton) */
  val onBlur:                      js.UndefOr[js.Function1[ReactFocusEvent, Unit]]                = js.undefined
  /** (Passed on to EnhancedButton) */
  val onFocus:                     js.UndefOr[js.Function1[ReactFocusEvent, Unit]]                = js.undefined
  /** (Passed on to EnhancedButton) */
  val onKeyDown:                   js.UndefOr[js.Function1[ReactKeyboardEvent, Unit]]             = js.undefined
  /** (Passed on to EnhancedButton) */
  val onKeyUp:                     js.UndefOr[js.Function1[ReactKeyboardEvent, Unit]]             = js.undefined
}


@js.native
trait MuiListItemM extends js.Object {
  def applyFocusState(focusState: MuiFocusedState): Unit /*One of none, focused, keyboardfocused*/ = js.native

  def createDisabledElement(styles: CssProperties, contentChildren: Array[CtorType.ChildArg], additionalProps: js.Any): Unit = js.native

  def createLabelElement(styles: CssProperties, contentChildren: Array[CtorType.ChildArg], additionalProps: js.Any): Unit = js.native

  def createTextElement(styles: CssProperties, data: js.Any, key: String): Unit = js.native

  def pushElement(children: Array[CtorType.ChildArg], element: js.Any, baseStyles: CssProperties, additionalProps: js.Any): Unit = js.native
}


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
