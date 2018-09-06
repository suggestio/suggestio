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
    
case class MuiTextField(
  key:                      js.UndefOr[String]                                     = js.undefined,
  ref:                      js.UndefOr[MuiTextFieldM => Unit]                      = js.undefined,
  /** The css class name of the root element. */
  className:                js.UndefOr[String]                                     = js.undefined,
  /** The text string to use for the default value. */
  defaultValue:             js.UndefOr[String]                                     = js.undefined,
  /** Disables the text field if set to true. */
  disabled:                 js.UndefOr[Boolean]                                    = js.undefined,
  /** The style object to use to override error styles. */
  errorStyle:               js.UndefOr[CssProperties]                              = js.undefined,
  /** The error content to display. */
  errorText:                js.UndefOr[VdomNode]                                   = js.undefined,
  /** If true, the floating label will float even when there is no value. */
  floatingLabelFixed:       js.UndefOr[Boolean]                                    = js.undefined,
  /** The style object to use to override floating label styles when focused. */
  floatingLabelFocusStyle:  js.UndefOr[CssProperties]                              = js.undefined,
  /** The style object to use to override floating label styles when shrunk. */
  floatingLabelShrinkStyle: js.UndefOr[CssProperties]                              = js.undefined,
  /** The style object to use to override floating label styles. */
  floatingLabelStyle:       js.UndefOr[CssProperties]                              = js.undefined,
  /** The content to use for the floating label element. */
  floatingLabelText:        js.UndefOr[VdomNode]                                   = js.undefined,
  /** If true, the field receives the property width 100%. */
  fullWidth:                js.UndefOr[Boolean]                                    = js.undefined,
  /** Override the inline-styles of the TextField's hint text element. */
  hintStyle:                js.UndefOr[CssProperties]                              = js.undefined,
  /** The hint content to display. */
  hintText:                 js.UndefOr[VdomNode]                                   = js.undefined,
  /** The id prop for the text field. */
  id:                       js.UndefOr[String]                                     = js.undefined,
  /** Override the inline-styles of the TextField's input element.
     When multiLine is false: define the style of the input element.
     When multiLine is true: define the style of the container of the textarea. */
  inputStyle:               js.UndefOr[CssProperties]                              = js.undefined,
  /** If true, a textarea element will be rendered.
     The textarea also grows and shrinks according to the number of lines. */
  multiLine:                js.UndefOr[Boolean]                                    = js.undefined,
  /** Name applied to the input. */
  name:                     js.UndefOr[String]                                     = js.undefined,
  onBlur:                   js.UndefOr[ReactFocusEventFromInput => Callback]       = js.undefined,
  /** Callback function that is fired when the textfield's value changes.
     @param {object} event Change event targeting the text field.
     @param {string} newValue The new value of the text field. */
  onChange:                 js.UndefOr[(ReactEventFromInput, String) => Callback]  = js.undefined,
  onFocus:                  js.UndefOr[ReactFocusEventFromInput => Callback]       = js.undefined,
  /** Number of rows to display when multiLine option is set to true. */
  rows:                     js.UndefOr[Double]                                     = js.undefined,
  /** Maximum number of rows to display when
     multiLine option is set to true. */
  rowsMax:                  js.UndefOr[Double]                                     = js.undefined,
  /** Override the inline-styles of the root element. */
  style:                    js.UndefOr[CssProperties]                              = js.undefined,
  /** Override the inline-styles of the TextField's textarea element.
     The TextField use either a textarea or an input,
     this property has effects only when multiLine is true. */
  textareaStyle:            js.UndefOr[CssProperties]                              = js.undefined,
  /** Specifies the type of input to display
     such as "password" or "text". */
  `type`:                   js.UndefOr[String]                                     = js.undefined,
  /** Override the inline-styles of the
     TextField's underline element when disabled. */
  underlineDisabledStyle:   js.UndefOr[CssProperties]                              = js.undefined,
  /** Override the inline-styles of the TextField's
     underline element when focussed. */
  underlineFocusStyle:      js.UndefOr[CssProperties]                              = js.undefined,
  /** If true, shows the underline for the text field. */
  underlineShow:            js.UndefOr[Boolean]                                    = js.undefined,
  /** Override the inline-styles of the TextField's underline element. */
  underlineStyle:           js.UndefOr[CssProperties]                              = js.undefined,
  /** The value of the text field. */
  value:                    js.UndefOr[String]                                     = js.undefined,
  /** (Passed on to EnhancedTextarea) */
  onHeightChange:           js.UndefOr[(ReactEvent, Int)=> Callback]               = js.undefined,
  /** (Passed on to EnhancedTextarea) */
  shadowStyle:              js.UndefOr[CssProperties]                              = js.undefined,
  /** (Passed on to EnhancedTextarea) */
  valueLink:                js.UndefOr[js.Any]                                     = js.undefined,
  /** (Passed on to DOM) */
  onAnimationEnd:           js.UndefOr[ReactEventFromInput => Callback]            = js.undefined,
  /** (Passed on to DOM) */
  onAnimationIteration:     js.UndefOr[ReactEventFromInput => Callback]            = js.undefined,
  /** (Passed on to DOM) */
  onAnimationStart:         js.UndefOr[ReactEventFromInput => Callback]            = js.undefined,
  /** (Passed on to DOM) */
  onClick:                  js.UndefOr[ReactMouseEventFromInput => Callback]       = js.undefined,
  /** (Passed on to DOM) */
  onCompositionEnd:         js.UndefOr[ReactCompositionEventFromInput => Callback] = js.undefined,
  /** (Passed on to DOM) */
  onCompositionStart:       js.UndefOr[ReactCompositionEventFromInput => Callback] = js.undefined,
  /** (Passed on to DOM) */
  onCompositionUpdate:      js.UndefOr[ReactCompositionEventFromInput => Callback] = js.undefined,
  /** (Passed on to DOM) */
  onContextMenu:            js.UndefOr[ReactEventFromInput => Callback]            = js.undefined,
  /** (Passed on to DOM) */
  onCopy:                   js.UndefOr[ReactClipboardEventFromInput => Callback]   = js.undefined,
  /** (Passed on to DOM) */
  onCut:                    js.UndefOr[ReactClipboardEventFromInput => Callback]   = js.undefined,
  /** (Passed on to DOM) */
  onDoubleClick:            js.UndefOr[ReactMouseEventFromInput => Callback]       = js.undefined,
  /** (Passed on to DOM) */
  onDrag:                   js.UndefOr[ReactDragEventFromInput => Callback]        = js.undefined,
  /** (Passed on to DOM) */
  onDragEnd:                js.UndefOr[ReactDragEventFromInput => Callback]        = js.undefined,
  /** (Passed on to DOM) */
  onDragEnter:              js.UndefOr[ReactDragEventFromInput => Callback]        = js.undefined,
  /** (Passed on to DOM) */
  onDragExit:               js.UndefOr[ReactDragEventFromInput => Callback]        = js.undefined,
  /** (Passed on to DOM) */
  onDragLeave:              js.UndefOr[ReactDragEventFromInput => Callback]        = js.undefined,
  /** (Passed on to DOM) */
  onDragOver:               js.UndefOr[ReactDragEventFromInput => Callback]        = js.undefined,
  /** (Passed on to DOM) */
  onDragStart:              js.UndefOr[ReactDragEventFromInput => Callback]        = js.undefined,
  /** (Passed on to DOM) */
  onDrop:                   js.UndefOr[ReactDragEventFromInput => Callback]        = js.undefined,
  /** (Passed on to DOM) */
  onInput:                  js.UndefOr[ReactKeyboardEventFromInput => Callback]    = js.undefined,
  /** (Passed on to DOM) */
  onKeyDown:                js.UndefOr[ReactKeyboardEventFromInput => Callback]    = js.undefined,
  /** (Passed on to DOM) */
  onKeyPress:               js.UndefOr[ReactKeyboardEventFromInput => Callback]    = js.undefined,
  /** (Passed on to DOM) */
  onKeyUp:                  js.UndefOr[ReactKeyboardEventFromInput => Callback]    = js.undefined,
  /** (Passed on to DOM) */
  onMouseDown:              js.UndefOr[ReactMouseEventFromInput => Callback]       = js.undefined,
  /** (Passed on to DOM) */
  onMouseEnter:             js.UndefOr[ReactMouseEventFromInput => Callback]       = js.undefined,
  /** (Passed on to DOM) */
  onMouseLeave:             js.UndefOr[ReactMouseEventFromInput => Callback]       = js.undefined,
  /** (Passed on to DOM) */
  onMouseMove:              js.UndefOr[ReactMouseEventFromInput => Callback]       = js.undefined,
  /** (Passed on to DOM) */
  onMouseUp:                js.UndefOr[ReactMouseEventFromInput => Callback]       = js.undefined,
  /** (Passed on to DOM) */
  onPaste:                  js.UndefOr[ReactClipboardEventFromInput => Callback]   = js.undefined,
  /** (Passed on to DOM) */
  onScroll:                 js.UndefOr[ReactUIEventFromInput => Callback]          = js.undefined,
  /** (Passed on to DOM) */
  onSelect:                 js.UndefOr[ReactUIEventFromInput => Callback]          = js.undefined,
  /** (Passed on to DOM) */
  onSubmit:                 js.UndefOr[ReactEventFromInput => Callback]            = js.undefined,
  /** (Passed on to DOM) */
  onTouchCancel:            js.UndefOr[ReactTouchEventFromInput => Callback]       = js.undefined,
  /** (Passed on to DOM) */
  onTouchEnd:               js.UndefOr[ReactTouchEventFromInput => Callback]       = js.undefined,
  /** (Passed on to DOM) */
  onTouchMove:              js.UndefOr[ReactTouchEventFromInput => Callback]       = js.undefined,
  /** (Passed on to DOM) */
  onTouchStart:             js.UndefOr[ReactTouchEventFromInput => Callback]       = js.undefined,
  /** (Passed on to DOM) */
  onTransitionEnd:          js.UndefOr[ReactTouchEventFromInput => Callback]       = js.undefined,
  /** (Passed on to DOM) */
  onWheel:                  js.UndefOr[ReactWheelEventFromInput => Callback]       = js.undefined,
  /** Specifies the types of files that the server accepts (only for type='file') */
  accept:                   js.UndefOr[String]                                     = js.undefined,
  /** Specifies an alternate text for images (only for type='image') */
  alt:                      js.UndefOr[String]                                     = js.undefined,
  /** Specifies whether an <input> element should have autocomplete enabled */
  autocomplete:             js.UndefOr[AutoComplete]                               = js.undefined,
  /** Specifies that an <input> element should automatically get focus when the page loads */
  autofocus:                js.UndefOr[Boolean]                                    = js.undefined,
  /** Specifies that an <input> element should be pre-selected when the page loads (for type='checkbox' or type='radio') */
  checked:                  js.UndefOr[Boolean]                                    = js.undefined,
  /** Specifies that the text direction will be submitted */
  dirname:                  js.UndefOr[Boolean]                                    = js.undefined,
  /** Specifies one or more forms the <input> element belongs to */
  form:                     js.UndefOr[String]                                     = js.undefined,
  /** Specifies the URL of the file that will process the input control when the form is submitted (for type='submit' and type='image') */
  formaction:               js.UndefOr[String]                                     = js.undefined,
  /** Specifies how the form-data should be encoded when submitting it to the server (for type='submit' and type='image') */
  formenctype:              js.UndefOr[InputEncodingType]                          = js.undefined,
  /** Defines the HTTP method for sending data to the action URL (for type='submit' and type='image') */
  formmethod:               js.UndefOr[FormMethodType]                             = js.undefined,
  /** Defines that form elements should not be validated when submitted */
  formnovalidate:           js.UndefOr[Boolean]                                    = js.undefined,
  /** Specifies where to display the response that is received after submitting the form (for type='submit' and type='image') */
  formtarget:               js.UndefOr[FormTarget]                                 = js.undefined,
  /** Specifies the height of an <input> element (only for type='image') */
  height:                   js.UndefOr[Int]                                        = js.undefined,
  /** Refers to a <datalist> element that contains pre-defined options for an <input> element */
  list:                     js.UndefOr[String]                                     = js.undefined,
  /** Specifies the maximum value for an <input> element */
  max:                      js.UndefOr[Float | String]                             = js.undefined,
  /** Specifies the maximum number of characters allowed in an <input> element */
  maxlength:                js.UndefOr[Int]                                        = js.undefined,
  /** Specifies a minimum value for an <input> element */
  min:                      js.UndefOr[Float | String]                             = js.undefined,
  /** Specifies that a user can enter more than one value in an <input> element */
  multiple:                 js.UndefOr[Boolean]                                    = js.undefined,
  /** Specifies a regular expression that an <input> element's value is checked against */
  pattern:                  js.UndefOr[String]                                     = js.undefined,
  /** Specifies a short hint that describes the expected value of an <input> element */
  placeholder:              js.UndefOr[String]                                     = js.undefined,
  /** Specifies that an input field is read-only */
  readonly:                 js.UndefOr[Boolean]                                    = js.undefined,
  /** Specifies that an input field must be filled out before submitting the form */
  required:                 js.UndefOr[Boolean]                                    = js.undefined,
  /** Specifies the width; in characters; of an <input> element */
  size:                     js.UndefOr[Int]                                        = js.undefined,
  /** Specifies the URL of the image to use as a submit button (only for type='image') */
  src:                      js.UndefOr[String]                                     = js.undefined,
  /** Specifies the legal number intervals for an input field */
  step:                     js.UndefOr[Float | String]                             = js.undefined,
  /** Specifies the width of an <input> element (only for type='image') */
  width:                    js.UndefOr[Int]                                        = js.undefined){


  def apply(children: VdomNode*) = {
    
    val props = JSMacro[MuiTextField](this)
    val f = JsComponent[js.Object, Children.Varargs, Null](Mui.TextField)
    f(props)(children: _*)
  }
}


@js.native
trait MuiTextFieldM extends js.Object {
  def blur(): Unit = js.native

  def focus(): Unit = js.native

  def getInputNode(): Unit = js.native

  def getValue(): String = js.native

  def select(): Unit = js.native
}
