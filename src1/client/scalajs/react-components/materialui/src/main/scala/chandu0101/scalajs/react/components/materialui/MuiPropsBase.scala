package chandu0101.scalajs.react.components.materialui

import scala.scalajs.js
import japgolly.scalajs.react._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.09.18 21:23
  */
trait MuiPropsBase extends js.Object {
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
