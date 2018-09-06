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
    
case class MuiFlatButton(
  key:                  js.UndefOr[String]                                 = js.undefined,
  ref:                  js.UndefOr[String]                                 = js.undefined,
  /** Color of button when mouse is not hovering over it. */
  backgroundColor:      js.UndefOr[MuiColors | MuiColor | String]          = js.undefined,
  /** The CSS class name of the root element. */
  className:            js.UndefOr[String]                                 = js.undefined,
  /** The element to use as the container for the FlatButton. Either a string to
     use a DOM element or a ReactElement. This is useful for wrapping the
     FlatButton in a custom Link component. If a ReactElement is given, ensure
     that it passes all of its given props through to the underlying DOM
     element and renders its children prop for proper integration. */
  containerElement:     js.UndefOr[String | VdomElement]                   = js.undefined,
  /** If true, the element's ripple effect will be disabled. */
  disableTouchRipple:   js.UndefOr[Boolean]                                = js.undefined,
  /** Disables the button if set to true. */
  disabled:             js.UndefOr[Boolean]                                = js.undefined,
  /** If true, the button will take up the full width of its container. */
  fullWidth:            js.UndefOr[Boolean]                                = js.undefined,
  /** Color of button when mouse hovers over. */
  hoverColor:           js.UndefOr[MuiColors | MuiColor | String]          = js.undefined,
  /** The URL to link to when the button is clicked. */
  href:                 js.UndefOr[String]                                 = js.undefined,
  /** Use this property to display an icon. */
  icon:                 js.UndefOr[VdomNode]                               = js.undefined,
  /** Label for the button. */
  label:                js.UndefOr[String]                                 = js.undefined,
  /** Place label before or after the passed children. */
  labelPosition:        js.UndefOr[BeforeAfter]                            = js.undefined,
  /** Override the inline-styles of the button's label element. */
  labelStyle:           js.UndefOr[CssProperties]                          = js.undefined,
  /** Callback function fired when the button is clicked.
     @param {object} event Click event targeting the button. */
  onClick:              js.UndefOr[ReactEvent => Callback]                 = js.undefined,
  /** Callback function fired when the element is focused or blurred by the keyboard.
     @param {object} event `focus` or `blur` event targeting the element.
     @param {boolean} isKeyboardFocused Indicates whether the element is focused. */
  onKeyboardFocus:      js.UndefOr[(ReactFocusEvent, Boolean) => Callback] = js.undefined,
  onMouseEnter:         js.UndefOr[ReactMouseEvent => Callback]            = js.undefined,
  onMouseLeave:         js.UndefOr[ReactMouseEvent => Callback]            = js.undefined,
  onTouchStart:         js.UndefOr[ReactTouchEvent => Callback]            = js.undefined,
  /** If true, colors button according to
     primaryTextColor from the Theme. */
  primary:              js.UndefOr[Boolean]                                = js.undefined,
  /** Color for the ripple after button is clicked. */
  rippleColor:          js.UndefOr[MuiColors | MuiColor | String]          = js.undefined,
  /** If true, colors button according to secondaryTextColor from the theme.
     The primary prop has precendent if set to true. */
  secondary:            js.UndefOr[Boolean]                                = js.undefined,
  /** Override the inline-styles of the root element. */
  style:                js.UndefOr[CssProperties]                          = js.undefined,
  /** (Passed on to EnhancedButton) */
  centerRipple:         js.UndefOr[Boolean]                                = js.undefined,
  /** (Passed on to EnhancedButton) */
  disableFocusRipple:   js.UndefOr[Boolean]                                = js.undefined,
  /** (Passed on to EnhancedButton) */
  disableKeyboardFocus: js.UndefOr[Boolean]                                = js.undefined,
  /** (Passed on to EnhancedButton) */
  focusRippleColor:     js.UndefOr[MuiColors | MuiColor | String]          = js.undefined,
  /** (Passed on to EnhancedButton) */
  focusRippleOpacity:   js.UndefOr[Double]                                 = js.undefined,
  /** (Passed on to EnhancedButton) */
  keyboardFocused:      js.UndefOr[Boolean]                                = js.undefined,
  /** (Passed on to EnhancedButton) */
  onBlur:               js.UndefOr[ReactFocusEvent => Callback]            = js.undefined,
  /** (Passed on to EnhancedButton) */
  onFocus:              js.UndefOr[ReactFocusEvent => Callback]            = js.undefined,
  /** (Passed on to EnhancedButton) */
  onKeyDown:            js.UndefOr[ReactKeyboardEvent => Callback]         = js.undefined,
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
    * @param children This is what will be displayed inside the button.
    If a label is specified, the text within the label prop will
    be displayed. Otherwise, the component will expect children
    which will then be displayed. (In our example,
    we are nesting an `<input type="file" />` and a `span`
    that acts as our label to be displayed.) This only
    applies to flat and raised buttons.
   */
  def apply(children: VdomNode*) = {
    
    val props = JSMacro[MuiFlatButton](this)
    val f = JsComponent[js.Object, Children.Varargs, Null](Mui.FlatButton)
    f(props)(children: _*)
  }
}
