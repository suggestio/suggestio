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
    
case class MuiDatePicker(
  key:                      js.UndefOr[String]                                     = js.undefined,
  ref:                      js.UndefOr[MuiDatePickerM => Unit]                     = js.undefined,
  /** Constructor for date formatting for the specified `locale`.
     The constructor must follow this specification: ECMAScript Internationalization API 1.0 (ECMA-402).
     `Intl.DateTimeFormat` is supported by most modern browsers, see http:
     otherwise https:
     By default, a built-in `DateTimeFormat` is used which supports the 'en-US' `locale`. */
  DateTimeFormat:           js.UndefOr[js.Function]                                = js.undefined,
  /** If true, automatically accept and close the picker on select a date. */
  autoOk:                   js.UndefOr[Boolean]                                    = js.undefined,
  /** Override the default text of the 'Cancel' button. */
  cancelLabel:              js.UndefOr[VdomNode]                                   = js.undefined,
  /** The css class name of the root element. */
  className:                js.UndefOr[String]                                     = js.undefined,
  /** Used to control how the Date Picker will be displayed when the input field is focused.
     `dialog` (default) displays the DatePicker as a dialog with a modal.
     `inline` displays the DatePicker below the input field (similar to auto complete). */
  container:                js.UndefOr[DialogInline]                               = js.undefined,
  /** This is the initial date value of the component.
     If either `value` or `valueLink` is provided they will override this
     prop with `value` taking precedence. */
  defaultDate:              js.UndefOr[js.Date]                                    = js.undefined,
  /** Override the inline-styles of DatePickerDialog's Container element. */
  dialogContainerStyle:     js.UndefOr[CssProperties]                              = js.undefined,
  /** Disables the year selection in the date picker. */
  disableYearSelection:     js.UndefOr[Boolean]                                    = js.undefined,
  /** Disables the DatePicker. */
  disabled:                 js.UndefOr[Boolean]                                    = js.undefined,
  /** Used to change the first day of week. It varies from
     Saturday to Monday between different locales.
     The allowed range is 0 (Sunday) to 6 (Saturday).
     The default is `1`, Monday, as per ISO 8601. */
  firstDayOfWeek:           js.UndefOr[Double]                                     = js.undefined,
  /** This function is called to format the date displayed in the input field, and should return a string.
     By default if no `locale` and `DateTimeFormat` is provided date objects are formatted to ISO 8601 YYYY-MM-DD.
     @param {object} date Date object to be formatted.
     @returns {any} The formatted date. */
  formatDate:               js.UndefOr[js.Date => String]                          = js.undefined,
  /** Hide date display */
  hideCalendarDate:         js.UndefOr[Boolean]                                    = js.undefined,
  /** Locale used for formatting the `DatePicker` date strings. Other than for 'en-US', you
     must provide a `DateTimeFormat` that supports the chosen `locale`. */
  locale:                   js.UndefOr[String]                                     = js.undefined,
  /** The ending of a range of valid dates. The range includes the endDate.
     The default value is current date + 100 years. */
  maxDate:                  js.UndefOr[js.Date]                                    = js.undefined,
  /** The beginning of a range of valid dates. The range includes the startDate.
     The default value is current date - 100 years. */
  minDate:                  js.UndefOr[js.Date]                                    = js.undefined,
  /** Tells the component to display the picker in portrait or landscape mode. */
  mode:                     js.UndefOr[PortraitLandscape]                          = js.undefined,
  /** Override the default text of the 'OK' button. */
  okLabel:                  js.UndefOr[VdomNode]                                   = js.undefined,
  /** Callback function that is fired when the date value changes.
     @param {null} null Since there is no particular event associated with the change,
     the first argument will always be null.
     @param {object} date The new date. */
  onChange:                 js.UndefOr[(js.UndefOr[Nothing], js.Date) => Callback] = js.undefined,
  /** Callback function that is fired when a click event occurs on the Date Picker's `TextField`.
     @param {object} event Click event targeting the `TextField`. */
  onClick:                  js.UndefOr[ReactEvent => Callback]                     = js.undefined,
  /** Callback function that is fired when the Date Picker's dialog is dismissed. */
  onDismiss:                js.UndefOr[Callback]                                   = js.undefined,
  /** Callback function that is fired when the Date Picker's `TextField` gains focus. */
  onFocus:                  js.UndefOr[ReactFocusEvent => Callback]                = js.undefined,
  /** Callback function that is fired when the Date Picker's dialog is shown. */
  onShow:                   js.UndefOr[Callback]                                   = js.undefined,
  /** If true sets the datepicker to open to year selection first. */
  openToYearSelection:      js.UndefOr[Boolean]                                    = js.undefined,
  /** Callback function used to determine if a day's entry should be disabled on the calendar.
     @param {object} day Date object of a day.
     @returns {boolean} Indicates whether the day should be disabled. */
  shouldDisableDate:        js.UndefOr[js.Date => Boolean]                         = js.undefined,
  /** Override the inline-styles of the root element. */
  style:                    js.UndefOr[CssProperties]                              = js.undefined,
  /** Override the inline-styles of DatePicker's TextField element. */
  textFieldStyle:           js.UndefOr[CssProperties]                              = js.undefined,
  /** This object should contain methods needed to build the calendar system.
     Useful for building a custom calendar system. Refer to the
     [source code](https:
     and an [example implementation](https:
     for more information. */
  utils:                    js.UndefOr[DatePickerUtils]                            = js.undefined,
  /** Sets the date for the Date Picker programmatically. */
  value:                    js.UndefOr[js.Date]                                    = js.undefined,
  /** The text string to use for the default value.
     (Passed on to TextField) */
  defaultValue:             js.UndefOr[String]                                     = js.undefined,
  /** The style object to use to override error styles.
     (Passed on to TextField) */
  errorStyle:               js.UndefOr[CssProperties]                              = js.undefined,
  /** The error content to display.
     (Passed on to TextField) */
  errorText:                js.UndefOr[VdomNode]                                   = js.undefined,
  /** If true, the floating label will float even when there is no value.
     (Passed on to TextField) */
  floatingLabelFixed:       js.UndefOr[Boolean]                                    = js.undefined,
  /** The style object to use to override floating label styles when focused.
     (Passed on to TextField) */
  floatingLabelFocusStyle:  js.UndefOr[CssProperties]                              = js.undefined,
  /** The style object to use to override floating label styles when shrunk.
     (Passed on to TextField) */
  floatingLabelShrinkStyle: js.UndefOr[CssProperties]                              = js.undefined,
  /** The style object to use to override floating label styles.
     (Passed on to TextField) */
  floatingLabelStyle:       js.UndefOr[CssProperties]                              = js.undefined,
  /** The content to use for the floating label element.
     (Passed on to TextField) */
  floatingLabelText:        js.UndefOr[VdomNode]                                   = js.undefined,
  /** If true, the field receives the property width 100%.
     (Passed on to TextField) */
  fullWidth:                js.UndefOr[Boolean]                                    = js.undefined,
  /** Override the inline-styles of the TextField's hint text element.
     (Passed on to TextField) */
  hintStyle:                js.UndefOr[CssProperties]                              = js.undefined,
  /** The hint content to display.
     (Passed on to TextField) */
  hintText:                 js.UndefOr[VdomNode]                                   = js.undefined,
  /** The id prop for the text field.
     (Passed on to TextField) */
  id:                       js.UndefOr[String]                                     = js.undefined,
  /** Override the inline-styles of the TextField's input element.
     When multiLine is false: define the style of the input element.
     When multiLine is true: define the style of the container of the textarea.
     (Passed on to TextField) */
  inputStyle:               js.UndefOr[CssProperties]                              = js.undefined,
  /** If true, a textarea element will be rendered.
     The textarea also grows and shrinks according to the number of lines.
     (Passed on to TextField) */
  multiLine:                js.UndefOr[Boolean]                                    = js.undefined,
  /** Name applied to the input.
     (Passed on to TextField) */
  name:                     js.UndefOr[String]                                     = js.undefined,
  /** (Passed on to TextField) */
  onBlur:                   js.UndefOr[ReactFocusEventFromInput => Callback]       = js.undefined,
  /** Number of rows to display when multiLine option is set to true.
     (Passed on to TextField) */
  rows:                     js.UndefOr[Double]                                     = js.undefined,
  /** Maximum number of rows to display when
     multiLine option is set to true.
     (Passed on to TextField) */
  rowsMax:                  js.UndefOr[Double]                                     = js.undefined,
  /** Override the inline-styles of the TextField's textarea element.
     The TextField use either a textarea or an input,
     this property has effects only when multiLine is true.
     (Passed on to TextField) */
  textareaStyle:            js.UndefOr[CssProperties]                              = js.undefined,
  /** Specifies the type of input to display
     such as "password" or "text".
     (Passed on to TextField) */
  `type`:                   js.UndefOr[String]                                     = js.undefined,
  /** Override the inline-styles of the
     TextField's underline element when disabled.
     (Passed on to TextField) */
  underlineDisabledStyle:   js.UndefOr[CssProperties]                              = js.undefined,
  /** Override the inline-styles of the TextField's
     underline element when focussed.
     (Passed on to TextField) */
  underlineFocusStyle:      js.UndefOr[CssProperties]                              = js.undefined,
  /** If true, shows the underline for the text field.
     (Passed on to TextField) */
  underlineShow:            js.UndefOr[Boolean]                                    = js.undefined,
  /** Override the inline-styles of the TextField's underline element.
     (Passed on to TextField) */
  underlineStyle:           js.UndefOr[CssProperties]                              = js.undefined,
  /** (Passed on to DOM) */
  onAnimationEnd:           js.UndefOr[ReactEventFromInput => Callback]            = js.undefined,
  /** (Passed on to DOM) */
  onAnimationIteration:     js.UndefOr[ReactEventFromInput => Callback]            = js.undefined,
  /** (Passed on to DOM) */
  onAnimationStart:         js.UndefOr[ReactEventFromInput => Callback]            = js.undefined,
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
    
    val props = JSMacro[MuiDatePicker](this)
    val f = JsComponent[js.Object, Children.Varargs, Null](Mui.DatePicker)
    f(props)(children: _*)
  }
}


@js.native
trait MuiDatePickerM extends js.Object {
  def focus(): Unit = js.native

  def getControlledDate(): js.Date = js.native

  def getDate(): js.Date = js.native

  def isControlled(): Unit = js.native

  def openDialog(): Unit = js.native
}
