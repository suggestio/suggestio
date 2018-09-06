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
    
case class MuiChip(
  key:                  js.UndefOr[String]                                 = js.undefined,
  ref:                  js.UndefOr[String]                                 = js.undefined,
  /** Override the background color of the chip. */
  backgroundColor:      js.UndefOr[MuiColors | MuiColor | String]          = js.undefined,
  /** CSS `className` of the root element. */
  className:            js.UndefOr[VdomNode]                               = js.undefined,
  /** The element to use as the container for the Chip. Either a string to
     use a DOM element or a ReactElement. */
  containerElement:     js.UndefOr[String | VdomElement]                   = js.undefined,
  /** Override the inline-styles of the delete icon. */
  deleteIconStyle:      js.UndefOr[CssProperties]                          = js.undefined,
  /** Override the label color. */
  labelColor:           js.UndefOr[MuiColors | MuiColor | String]          = js.undefined,
  /** Override the inline-styles of the label. */
  labelStyle:           js.UndefOr[CssProperties]                          = js.undefined,
  onBlur:               js.UndefOr[ReactFocusEvent => Callback]            = js.undefined,
  /** Callback function fired when the `Chip` element is clicked.
     @param {object} event Click event targeting the element. */
  onClick:              js.UndefOr[ReactEvent => Callback]                 = js.undefined,
  onFocus:              js.UndefOr[ReactFocusEvent => Callback]            = js.undefined,
  onKeyDown:            js.UndefOr[ReactKeyboardEvent => Callback]         = js.undefined,
  onKeyboardFocus:      js.UndefOr[(ReactFocusEvent, Boolean) => Callback] = js.undefined,
  onMouseDown:          js.UndefOr[ReactMouseEvent => Callback]            = js.undefined,
  onMouseEnter:         js.UndefOr[ReactMouseEvent => Callback]            = js.undefined,
  onMouseLeave:         js.UndefOr[ReactMouseEvent => Callback]            = js.undefined,
  onMouseUp:            js.UndefOr[ReactMouseEvent => Callback]            = js.undefined,
  /** Callback function fired when the delete icon is clicked. If set, the delete icon will be shown.
     @param {object} event `click` event targeting the element. */
  onRequestDelete:      js.UndefOr[ReactTouchEvent => Callback]            = js.undefined,
  onTouchEnd:           js.UndefOr[ReactTouchEvent => Callback]            = js.undefined,
  onTouchStart:         js.UndefOr[ReactTouchEvent => Callback]            = js.undefined,
  /** Override the inline-styles of the root element. */
  style:                js.UndefOr[CssProperties]                          = js.undefined,
  /** (Passed on to EnhancedButton) */
  centerRipple:         js.UndefOr[Boolean]                                = js.undefined,
  /** (Passed on to EnhancedButton) */
  disableFocusRipple:   js.UndefOr[Boolean]                                = js.undefined,
  /** (Passed on to EnhancedButton) */
  disableKeyboardFocus: js.UndefOr[Boolean]                                = js.undefined,
  /** (Passed on to EnhancedButton) */
  disableTouchRipple:   js.UndefOr[Boolean]                                = js.undefined,
  /** (Passed on to EnhancedButton) */
  disabled:             js.UndefOr[Boolean]                                = js.undefined,
  /** (Passed on to EnhancedButton) */
  focusRippleColor:     js.UndefOr[MuiColors | MuiColor | String]          = js.undefined,
  /** (Passed on to EnhancedButton) */
  focusRippleOpacity:   js.UndefOr[Double]                                 = js.undefined,
  /** (Passed on to EnhancedButton) */
  href:                 js.UndefOr[String]                                 = js.undefined,
  /** (Passed on to EnhancedButton) */
  keyboardFocused:      js.UndefOr[Boolean]                                = js.undefined,
  /** (Passed on to EnhancedButton) */
  onKeyUp:              js.UndefOr[ReactKeyboardEvent => Callback]         = js.undefined,
  /** (Passed on to EnhancedButton) */
  tabIndex:             js.UndefOr[Double]                                 = js.undefined,
  /** (Passed on to EnhancedButton) */
  touchRippleColor:     js.UndefOr[MuiColors | MuiColor | String]          = js.undefined,
  /** (Passed on to EnhancedButton) */
  touchRippleOpacity:   js.UndefOr[Double]                                 = js.undefined,
  /** (Passed on to EnhancedButton) */
  `type`:               js.UndefOr[String]                                 = js.undefined){

  /**
    * @param children Used to render elements inside the Chip.
   */
  def apply(children: VdomNode*) = {
    
    val props = JSMacro[MuiChip](this)
    val f = JsComponent[js.Object, Children.Varargs, Null](Mui.Chip)
    f(props)(children: _*)
  }
}
