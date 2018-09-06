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
    
case class MuiRaisedButton(
  key:                     js.UndefOr[String]                                 = js.undefined,
  ref:                     js.UndefOr[String]                                 = js.undefined,
  /** Override the default background color for the button,
     but not the default disabled background color
     (use `disabledBackgroundColor` for this). */
  backgroundColor:         js.UndefOr[MuiColors | MuiColor | String]          = js.undefined,
  /** Override the inline-styles of the button element. */
  buttonStyle:             js.UndefOr[CssProperties]                          = js.undefined,
  /** The CSS class name of the root element. */
  className:               js.UndefOr[String]                                 = js.undefined,
  /** The element to use as the container for the RaisedButton. Either a string to
     use a DOM element or a ReactElement. This is useful for wrapping the
     RaisedButton in a custom Link component. If a ReactElement is given, ensure
     that it passes all of its given props through to the underlying DOM
     element and renders its children prop for proper integration. */
  containerElement:        js.UndefOr[String | VdomElement]                   = js.undefined,
  /** If true, the element's ripple effect will be disabled. */
  disableTouchRipple:      js.UndefOr[Boolean]                                = js.undefined,
  /** If true, the button will be disabled. */
  disabled:                js.UndefOr[Boolean]                                = js.undefined,
  /** Override the default background color for the button
     when it is disabled. */
  disabledBackgroundColor: js.UndefOr[MuiColors | MuiColor | String]          = js.undefined,
  /** The color of the button's label when the button is disabled. */
  disabledLabelColor:      js.UndefOr[MuiColors | MuiColor | String]          = js.undefined,
  /** If true, the button will take up the full width of its container. */
  fullWidth:               js.UndefOr[Boolean]                                = js.undefined,
  /** The URL to link to when the button is clicked. */
  href:                    js.UndefOr[String]                                 = js.undefined,
  /** An icon to be displayed within the button. */
  icon:                    js.UndefOr[VdomNode]                               = js.undefined,
  /** The label to be displayed within the button.
     If content is provided via the `children` prop, that content will be
     displayed in addition to the label provided here. */
  label:                   js.UndefOr[String]                                 = js.undefined,
  /** The color of the button's label. */
  labelColor:              js.UndefOr[MuiColors | MuiColor | String]          = js.undefined,
  /** The position of the button's label relative to the button's `children`. */
  labelPosition:           js.UndefOr[BeforeAfter]                            = js.undefined,
  /** Override the inline-styles of the button's label element. */
  labelStyle:              js.UndefOr[CssProperties]                          = js.undefined,
  /** Callback function fired when the button is clicked.
     @param {object} event Click event targeting the button. */
  onClick:                 js.UndefOr[ReactEvent => Callback]                 = js.undefined,
  onMouseDown:             js.UndefOr[ReactMouseEvent => Callback]            = js.undefined,
  onMouseEnter:            js.UndefOr[ReactMouseEvent => Callback]            = js.undefined,
  onMouseLeave:            js.UndefOr[ReactMouseEvent => Callback]            = js.undefined,
  onMouseUp:               js.UndefOr[ReactMouseEvent => Callback]            = js.undefined,
  onTouchEnd:              js.UndefOr[ReactTouchEvent => Callback]            = js.undefined,
  onTouchStart:            js.UndefOr[ReactTouchEvent => Callback]            = js.undefined,
  /** Override the inline style of the button overlay. */
  overlayStyle:            js.UndefOr[CssProperties]                          = js.undefined,
  /** If true, the button will use the theme's primary color. */
  primary:                 js.UndefOr[Boolean]                                = js.undefined,
  /** Override the inline style of the ripple element. */
  rippleStyle:             js.UndefOr[CssProperties]                          = js.undefined,
  /** If true, the button will use the theme's secondary color.
     If both `secondary` and `primary` are true, the button will use
     the theme's primary color. */
  secondary:               js.UndefOr[Boolean]                                = js.undefined,
  /** Override the inline-styles of the root element. */
  style:                   js.UndefOr[CssProperties]                          = js.undefined,
  /** (Passed on to EnhancedButton) */
  centerRipple:            js.UndefOr[Boolean]                                = js.undefined,
  /** (Passed on to EnhancedButton) */
  disableFocusRipple:      js.UndefOr[Boolean]                                = js.undefined,
  /** (Passed on to EnhancedButton) */
  disableKeyboardFocus:    js.UndefOr[Boolean]                                = js.undefined,
  /** (Passed on to EnhancedButton) */
  focusRippleColor:        js.UndefOr[MuiColors | MuiColor | String]          = js.undefined,
  /** (Passed on to EnhancedButton) */
  focusRippleOpacity:      js.UndefOr[Double]                                 = js.undefined,
  /** (Passed on to EnhancedButton) */
  keyboardFocused:         js.UndefOr[Boolean]                                = js.undefined,
  /** (Passed on to EnhancedButton) */
  onBlur:                  js.UndefOr[ReactFocusEvent => Callback]            = js.undefined,
  /** (Passed on to EnhancedButton) */
  onFocus:                 js.UndefOr[ReactFocusEvent => Callback]            = js.undefined,
  /** (Passed on to EnhancedButton) */
  onKeyDown:               js.UndefOr[ReactKeyboardEvent => Callback]         = js.undefined,
  /** (Passed on to EnhancedButton) */
  onKeyUp:                 js.UndefOr[ReactKeyboardEvent => Callback]         = js.undefined,
  /** (Passed on to EnhancedButton) */
  onKeyboardFocus:         js.UndefOr[(ReactFocusEvent, Boolean) => Callback] = js.undefined,
  /** (Passed on to EnhancedButton) */
  tabIndex:                js.UndefOr[Double]                                 = js.undefined,
  /** (Passed on to EnhancedButton) */
  touchRippleColor:        js.UndefOr[MuiColors | MuiColor | String]          = js.undefined,
  /** (Passed on to EnhancedButton) */
  touchRippleOpacity:      js.UndefOr[Double]                                 = js.undefined,
  /** (Passed on to EnhancedButton) */
  `type`:                  js.UndefOr[String]                                 = js.undefined){

  /**
    * @param children The content of the button.
    If a label is provided via the `label` prop, the text within the label
    will be displayed in addition to the content provided here.
   */
  def apply(children: VdomNode*) = {
    
    val props = JSMacro[MuiRaisedButton](this)
    val f = JsComponent[js.Object, Children.Varargs, Null](Mui.RaisedButton)
    f(props)(children: _*)
  }
}
